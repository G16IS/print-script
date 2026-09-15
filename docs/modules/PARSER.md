# Módulo `parser`

Dependencias: `common`, `lexer`. Tests también tiran de `infrastructure` para cargar `grammar.config.v1.0.json`.

Análisis **sintáctico** solamente. Tokens → `SyntaxProgram`. No chequea tipos ni declaraciones.

Dirigido por datos: el Kotlin es un evaluador de `Grammar`; PrintScript vive en el JSON.

---

## Cuándo tocarlo

- Combinadores (`or`/`seq`/`left`/`atom`/`repeat`/`optional`) o uno nuevo
- Mensajes / política de error (`null` vs `ParseException`)
- Backtracking del cursor
- Forma de los nodos (`wrap`, `binary`, qué se captura)
- **No** para agregar `if` si alcanza con JSON (`repeat` + `seq` ya existen)

---

## API pública

```kotlin
interface Parser {
    fun parseNextStatement(tokenStream: Lexer, program: SyntaxProgram): SyntaxProgram
}

object DefaultParserFactory {
    fun create(grammar: Grammar): Parser
}
```

Una llamada parsea **un** statement (`grammar.start`, hoy `"statement"`) y lo appendea al programa.

`DefaultParser` cachea un `LexerTokenSource` atado a la **identidad** del `Lexer`. Llamadas sucesivas con el mismo lexer reanudan el cursor. Un lexer distinto crea un source nuevo.

Si `evaluate(start)` da `null` → `ParseException` “Unexpected token …; expected start of statement”.

---

## Mapa de archivos

```
parser/src/main/kotlin/printscript/
  Parser.kt
  DefaultParser.kt
  DefaultParserFactory.kt
  token/
    TokenSource.kt          peek / advance / checkpoint / restore
    LexerTokenSource.kt     buffer sobre Lexer; EOF = type == "EOF"
  parse/
    RuleEvaluator.kt        despacha al handler que supports(rule)
    RuleHandler.kt
    RuleHandlers.kt         defaults(): Atom, Seq, Or, Left, Repeat, Optional
    ParseContext.kt         grammar + tokens + evaluate/tryEvaluate
    AtomRuleHandler.kt
    SeqRuleHandler.kt
    OrRuleHandler.kt
    LeftRuleHandler.kt
    RepeatRuleHandler.kt
    OptionalRuleHandler.kt
    step/
      StepEvaluator.kt      TokenStep / RuleRefStep
      StepOutcome.kt        matched + node? + location?
  error/
    ParseException.kt       RuntimeException con Location en el mensaje
    ParseErrors.kt          unexpectedToken / unexpectedStart
  util/
    Nodes.kt                tokenLeaf / wrap / binary
    Locations.kt            span de una lista de nodos
```

---

## Cursor (`TokenSource`)

El parser **no** llama `Lexer` directo salvo al llenar el buffer.

```kotlin
interface TokenSource {
    fun peek(): Token
    fun peek(offset: Int): Token
    fun advance(): Token
    fun isAtEnd(): Boolean          // peek().type == "EOF"
    fun checkpoint(): Int
    fun restore(mark: Int)
}
```

`LexerTokenSource` acumula tokens en una lista y mueve `index`. `restore` no relee el lexer: solo retrocede el índice. Imprescindible para `or` y para el primer step de `seq`.

`advance()` sobre EOF **no** incrementa (se queda en EOF).

---

## Evaluación de reglas

`RuleEvaluator.evaluate(ruleName, tokens)`:

1. `grammar.rule(name)` (falla si no existe)
2. primer handler con `supports(rule)`
3. `handler.evaluate(name, rule, ParseContext)`

`ParseContext.evaluate` recorre en caliente (puede dejar el cursor avanzado). `tryEvaluate` hace checkpoint, evalúa, y si da `null` restaura.

No hay memoization ni left-recursion general. La recusión izquierda se modela con `LeftRule` (loop, no recurse on self).

---

## Handlers

### `AtomRuleHandler`

Si `peek().type == atom.token`: `advance()`, nodo hoja `{ name = ruleName, token, location }`. Si no: `null`, no consume.

### `OrRuleHandler`

Prueba alternativas en orden con `tryEvaluate`. Devuelve el **nodo de la alternativa**, no un wrapper `or`. Por eso un statement `let …` se llama `"variable"`, no `"statement"`.

Si ninguna matchea: `null`.

### `SeqRuleHandler`

Corre los `SeqStep` en orden.

- Step 0 falla → restore del checkpoint de la seq, `null` (el `or` padre puede probar otra cosa)
- Step `i > 0` falla → `ParseException` (“Expected COLON, found TYPE”). **No** vuelve atrás para probar otra alternativa del `or`. Por eso `let x number = 1;` no se reinterpreta como expression-stmt: ya consumió `LET`

Hijos del nodo seq: solo steps que produjeron `SyntaxNode` (capturas y rule refs). `"LET"` y `";"` no aparecen.

Location: span del primer al último step que tuvo location, incluyendo no-capturados.

### `LeftRuleHandler`

1. Evalúa `left` (ej. `term`). Si `null`, la left rule falla.
2. Mientras `peek` sea `op.token` **y** `peek.value` esté en `op.values`: consume el operador, evalúa otro `left`, arma binario.
3. Si no hubo ningún operador: `wrap(name, first)` — igual hay un nodo `expression` con un hijo `term`.
4. Si el operador está pero falta el operando derecho: `ParseException`.

Binario:

```
SyntaxNode(name,
  children = [acc, tokenLeaf(op), right],
  location = span)
```

Asociatividad izquierda: `1 - 2 - 3` → `(- (- 1 2) 3)`.

`expression` solo consume `+` `-`; `*` queda para `term`. Por eso `1 + 2 * 3` es `+` en el nivel `expression` y `*` adentro del `term` derecho.

Un operador de otro nivel no se consume: `1 * 2` evaluado como `expression` envuelve un `term` y deja `*` en el cursor.

### `RepeatRuleHandler`

Siempre “matchea” (incluso cero veces). Loop de `tryEvaluate(item)` hasta `null`.

Si una iteración matchea **sin** avanzar el cursor → `IllegalStateException` (repeat infinito).

Location: span de los items, o la location del token actual si la lista está vacía.

`repeat` no está en el `grammar.config.v1.0.json` v1. Tests del parser cubren bloques `{ stmt* }` a mano. Listo para `if`.

### `OptionalRuleHandler`

Siempre envuelve. `tryEvaluate(item)`:

- `Matched` → nodo con nombre de la regla optional y 1 hijo
- `Missing` → mismo nodo con 0 hijos (no es miss de la optional)
- `Failed` → se propaga

v1 lo usa: `initializer` = `{ "optional": "var-init" }`.

---

## Steps de `seq` (`StepEvaluator`)

| Step | Match | Nodo |
|---|---|---|
| `TokenStep` type ok, `capture=false` | consume | sin nodo; location del token |
| `TokenStep` type ok, `capture=true` | consume | `tokenLeaf` (`name = token.type`) |
| `TokenStep` type distinto | miss | — |
| `RuleRefStep` | `ctx.evaluate(name)` | el subárbol o miss |

---

## Forma de los nodos (PrintScript)

Helpers de test: `lhs() = children[0]`, `op() = children[1].value()`, `rhs() = children[2]`.

`println(x);` queda:

```
expression-stmt
  expression
    term
      call
        CALL("println")
        expression
          term
            identifier  → token ID("x")
```

`identifier` es un `AtomRule` llamado `"identifier"` sobre token `"ID"`. El nodo se llama `"identifier"` y `value()` es el lexema. En un `variable`, el id se captura como `{ "capture": "ID" }`, entonces el hijo se llama `"ID"`, no `"identifier"`. No los mezcles al caminar el árbol.

---

## Errores

`ParseException(message, location)` formatea `"… at line L, column C"`.

| Caso | Resultado |
|---|---|
| Stream que no arranca un statement (`:`, EOF, …) | `unexpectedStart` |
| Seq incompleta | `unexpectedToken(peek, expected)` |
| Left sin operando derecho | idem |
| Regla desconocida | `IllegalStateException` de `Grammar.rule` |
| Repeat que no consume | `IllegalStateException` |

El parser **acepta** `let x: number = "hola";`. Eso es semántica.

---

## Tests

Harness en `parser/src/test/kotlin/printscript/support/`:

- `Grammars.kt` — `grammar()`, `atom()`, `seq()`, `expect()`, `capture()`, `ref()`, `left()`, `or()`, `repeat()`, `optional()`
- `Tokens.kt` — tokens sintéticos 1-based
- `MockLexer.kt` — `Lexer` de una lista (agrega EOF)
- `ParseSupport.kt` — `parse(grammar, *tokens)` sobre el start
- `PrintScriptGrammar` — resource real `/grammar.config.json`
- `Nodes.kt` — `lhs`/`op`/`rhs`

`ParserTest` es el contrato de PrintScript: let, println, precedencia, parens, asociatividad, errores, y “no rechaza mismatch de tipos”.

Cada handler tiene su test. `GrammarTest` cubre la validación de `common`. `SyntaxNodeTest` cubre `child`/`find`/`value`.

---

## Cómo extender

Producción nueva (`if`, args múltiples): JSON primero. `COMMA` ya se tokeniza. `repeat` ya parsea bloques.

Combinador nuevo: ver receta en [CONTEXT.md](../CONTEXT.md). Registrar el handler en `RuleHandlers.defaults()` **en un orden irrelevante** (se elige por `supports`, no por posición), pero no dejes dos handlers que acepten el mismo tipo.

No metas nombres `"LET"` en un handler. Si el handler necesita un token concreto, esa regla es un `AtomRule`/`TokenStep` en JSON.
