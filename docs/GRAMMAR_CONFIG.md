# Parser — evaluador de gramática

El parser es **solo sintaxis**. No conoce `let`, `println` ni tipos: evalúa las reglas de una `Grammar` (el modelo de `grammar.config.json`) contra un stream de tokens genéricos (`Token.type: String`).

Sale un árbol de sintaxis (`SyntaxNode` / `SyntaxProgram`), no el AST tipado de `common`.

```
Lexer → TokenSource (cursor + checkpoint)
              ↓
       RuleEvaluator
              ↓  un handler por tipo de regla
   atom | seq | or | left | repeat
              ↓
         SyntaxNode
```

## Cableado

```kotlin
val parser = DefaultParserFactory.create(grammar)
var program = SyntaxProgram.empty()
while (!lexer.peek(null).type.equals("EOF")) {
    program = parser.parseNextStatement(lexer, program)
}
```

La gramática la construye quien lee el JSON (próximo paso en `infrastructure`). Este módulo no parsea JSON: no hay dependencias nuevas. La interfaz `GrammarReader` es el gancho.

```kotlin
interface GrammarReader {
    fun read(json: String): Grammar
}
```

## Schema de `grammar.config.json`

```json
{
  "start": "statement",
  "rules": {
    "statement": { "or": ["variable", "expression-stmt"] },
    "number": { "atom": "NUMBER_LITERAL" }
  }
}
```

- `start`: nombre de la regla que arranca cada `parseNextStatement`.
- `rules`: mapa nombre → definición.
- Los tipos de token (`LET`, `ID`, `OPERATOR`, …) son **strings**. Tienen que coincidir con `language.config.json`.

## Tipos de regla

| JSON | Modelo | Qué hace |
|---|---|---|
| `{ "or": ["a", "b"] }` | `OrRule` | Primera alternativa que matchea. **No envuelve**: el nodo se llama como la alternativa (`variable`, no `statement`). |
| `{ "atom": "NUMBER_LITERAL" }` | `AtomRule` | Un token de ese tipo. Nodo con el nombre de la regla y el token. |
| `{ "seq": [ ... ] }` | `SeqRule` | Pasos en orden. Hijos = solo `capture` y `rule`. La puntuación se consume y se tira. |
| `{ "left": "term", "op": { "token": "OPERATOR", "values": ["+","-"] } }` | `LeftRule` | Operadores infijos asociativos a izquierda. Siempre produce un nodo con el nombre de la regla. |
| `{ "repeat": "statement" }` | `RepeatRule` | Cero o más `item`. **Implementado y testeado; el JSON v1 no lo usa.** |

### Steps de `seq`

| JSON | Modelo | ¿Queda en el árbol? |
|---|---|---|
| `"LET"` | `TokenStep("LET", capture=false)` | No |
| `{ "capture": "ID" }` | `TokenStep("ID", capture=true)` | Sí, leaf llamado `ID` |
| `{ "rule": "expression" }` | `RuleRefStep("expression")` | Sí, el nodo de esa regla |

## Árbol

```kotlin
data class SyntaxNode(
    val name: String,          // nombre de regla, o tipo de token si es capture
    val token: Token? = null,  // atoms y captures
    val children: List<SyntaxNode> = emptyList(),
    val location: Location
)
```

`let x: number = 1 + 2;`

```
variable
  ├─ ID("x")
  ├─ TYPE("number")
  └─ expression
       ├─ term / number("1")
       ├─ OPERATOR("+")
       └─ term / number("2")
```

El parser **no** convierte `"1"` a `Double`, **no** saca comillas, **no** resuelve `TYPE` a un enum.

## Backtracking y commit

`TokenSource` tiene `checkpoint()` / `restore()`.

| Situación | Resultado |
|---|---|
| Primer step de `seq` no matchea | restore + miss (`null`) |
| Step posterior de `seq` falla | `ParseException` |
| `atom` tipo incorrecto | no consume, `null` |
| `or`: alternativa miss | siguiente; si ninguna, `null` |
| `or`: alternativa tira | se propaga |
| `left`: operando inicial miss | `null` |
| `left`: operador sí, operando no | `ParseException` |
| `repeat`: ítem miss | se corta (éxito, incluso con 0 ítems) |

Nombre de regla inexistente → `IllegalStateException` (config rota), no error de parseo.

## Bloques (listo, no cableado)

`RepeatRuleHandler` ya parsea listas de statements. Para `if` / `while` / `for` alcanza con agregar reglas al JSON (y los tokens en el lexer). Ejemplo, **no implementado**:

```json
"block": { "repeat": "statement" },
"if": {
  "seq": [
    "IF",
    "LEFT_PAREN",
    { "rule": "expression" },
    "RIGHT_PAREN",
    "LEFT_BRACE",
    { "rule": "block" },
    "RIGHT_BRACE"
  ]
}
```

`repeat` se detiene cuando `statement` no arranca (p. ej. `}`). El `seq` del `if` consume `RIGHT_BRACE`.

También hay que sumar `if` a `statement.or` y tokens `IF` / `LEFT_BRACE` / `RIGHT_BRACE` en `language.config.json`.

## Cómo agregar un tipo de regla

1. Data class que implemente `GrammarRule` (con `references()`).
2. `RuleHandler` (`supports` + `evaluate`).
3. Registrarlo en `RuleHandlers.defaults()`.
4. Test con una gramática mínima (ver abajo).
5. Documentar el JSON acá.

El evaluador no se toca.

## Cómo agregar un test

El DSL de `parser/src/test/kotlin/printscript/support` es 1:1 con el JSON:

```kotlin
val g = grammar(
    "s",
    "s" to seq(expect("A"), capture("B"), ref("n")),
    "n" to atom("NUMBER_LITERAL")
)
val node = parse(g, Tokens.of("A"), Tokens.of("B", "x"), Tokens.number("1"))
```

- Handlers: gramáticas chicas en `evaluator/*RuleHandlerTest`.
- Lenguaje v1: `PrintScriptGrammar` (espejo del JSON) en `ParserTest`.

## Qué no hace este módulo

- Semántica (tipos, variables no declaradas).
- Leer `grammar.config.json` (falta `kotlinx-serialization-json` o tocar `infrastructure`).
- Mapear a `VariableStatement` / `CallExpression`.
- `if` / `while` / `for` en la gramática v1.
