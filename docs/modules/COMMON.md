# Módulo `common`

Dependencias Gradle: ninguna. Todos los demás módulos dependen de este.

Contrato compartido del lenguaje: tokens, gramática, árboles, posiciones, y **puertos** de I/O (interfaces). No hay implementación de lexer, parser, JSON ni filesystem. No uses kotlinx.serialization acá.

Si un tipo lo necesitan dos módulos, vive acá. Si un tipo es detalle de matching o de un handler, no.

---

## Cuándo tocarlo

- Nueva forma de regla de gramática o de token (`GrammarRule`, `TokenRule`, `SeqStep`)
- Cambiar la forma de `SyntaxNode` / `Token` / `Location`
- Nuevo puerto (`CodeReader`, config readers)
- **No** para serializers, matching de regex, ni lógica de tipos

---

## Mapa de archivos

```
common/src/main/kotlin/printscript/
  domain/
    Token.kt              Token(type: String, value: Optional<String>, location)
    TokenType.kt          enum leftover — no usar
    TokenRule.kt          ExactRule / RegexRule
    LanguageConfig.kt     order + config (categorías → reglas)
    Grammar.kt            start + rules, valida referencias
    GrammarRule.kt        Or / Atom / Seq / Left / Repeat
    SeqStep.kt            TokenStep / RuleRefStep
    OperatorSpec.kt       token type + valores de operador (para LeftRule)
  syntax/
    SyntaxNode.kt         árbol genérico de salida del parser
    SyntaxProgram.kt      lista de statements + location
  ast/
    ASTDefinition.kt      AST tipado leftover — lo usa Gradle :semantic (migrar a type-checker)
    Location.kt           start/end CharPosition
    VariableType.kt       NUMBER / STRING
  reader/
    CodeReader.kt         puerto: read / peek / currentPosition
    CharPosition.kt       (line, col)
    LanguageConfigReader.kt
    GrammarConfigReader.kt
  util/
    Utils.kt              readResource(classpath) — helper suelto
```

---

## Dominio léxico

### `Token`

```kotlin
data class Token(
    val type: String,              // "LET", "ID", "NUMBER_LITERAL", "EOF", …
    val value: Optional<String>,   // presente solo si la regla tenía capture=true
    val location: Location
)
```

`type` es un string libre. Tiene que coincidir entre `language.config.json` (`token`) y `grammar.config.json` (`atom`, steps de `seq`, `op.token` de `left`).

`TokenType` (enum) es un leftover de un diseño anterior. Nadie lo usa. Código nuevo: strings, igual que el JSON.

### `TokenRule`

```kotlin
sealed interface TokenRule {
    val matcher: List<String>
    val token: String
    val capture: Boolean
}

data class ExactRule(...) : TokenRule          // keywords, puntuación
data class RegexRule(..., val partial: String) : TokenRule  // ids, literales
```

El lexer decide `VALID` / `PARTIAL` / `INVALID` con estas reglas. `common` no evalúa nada: solo el modelo.

### `LanguageConfig`

```kotlin
data class LanguageConfig(
    val order: List<String>,                       // categorías, prioridad = índice
    val config: Map<String, List<TokenRule>>       // nombre de categoría → reglas
)
```

`order` **no significa “primero gana”** en el código actual. `RuleDrawResolver` (lexer) usa `order.indexOf(category)` y se queda con el **máximo**. Ver [LEXER.md](LEXER.md).

---

## Dominio de gramática

### `Grammar`

```kotlin
data class Grammar(val start: String, val rules: Map<String, GrammarRule>)
```

En el `init`:

1. `start` tiene que existir en `rules`
2. Todas las referencias (`OrRule.alternatives`, `SeqRule` → `RuleRefStep`, `LeftRule.left`, `RepeatRule.item`) tienen que existir

Si falla, tira `IllegalArgumentException`. Por eso `JSONGrammarConfigReader` termina construyendo un `Grammar` real: la validación corre al deserializar.

`grammar.rule(name)` tira si el nombre no existe (el parser lo usa).

### `GrammarRule`

| Tipo | Qué significa | `references()` |
|---|---|---|
| `OrRule(alternatives)` | primera alternativa que matchea | las alternativas |
| `AtomRule(token)` | un token de ese `type` | vacío |
| `SeqRule(steps)` | secuencia de `SeqStep` | nombres en `RuleRefStep` |
| `LeftRule(left, op)` | `left` (op `left`)* infix | `left` |
| `RepeatRule(item)` | `item*` (cero o más) | `item` |

### `SeqStep`

| Tipo | JSON | Efecto en el árbol |
|---|---|---|
| `TokenStep(type, capture=false)` | `"LET"` | consume y **descarta** (igual cuenta para location) |
| `TokenStep(type, capture=true)` | `{ "capture": "ID" }` | hoja `SyntaxNode(name=token.type, token=…)` |
| `RuleRefStep(name)` | `{ "rule": "expression" }` | subárbol de esa regla |

`OperatorSpec(token, values)` restringe un `LeftRule` a ciertos lexemas (`OPERATOR` con `+`/`-`, no `*`).

---

## Árbol que produce el parser: `syntax/`

Este es el árbol **vivo** del pipeline.

```kotlin
data class SyntaxNode(
    val name: String,                 // nombre de regla, o Token.type si es hoja capturada
    val token: Token? = null,
    val children: List<SyntaxNode> = emptyList(),
    val location: Location
)
```

API de recorrido (la que debe usar el type-checker):

- `child(name)` / `childOrNull(name)` — hijo directo por `name`
- `find(name)` / `findOrNull(name)` — DFS, incluye `this`
- `value()` — `token.value`; falla si no hay token con valor

`SyntaxProgram` es la lista de statements más el span. `withStatement` concatena y ajusta `location.start` al primer statement.

No hay tipos `VariableStatement` acá. Un `let` es un nodo `name = "variable"` cuyos hijos salen de los steps capturados y las reglas anidadas.

---

## AST tipado: `ast/ASTDefinition.kt`

Modelo anterior, **todavía usado por Gradle `:semantic`**. El type-checker tiene que dejarlo atrás y caminar `SyntaxNode`. Jerarquía:

```
Node
  Program
  Statement
    VariableStatement(declaration: VariableDeclaration)
    ExpressionStmt
      ExpressionStatement(expression)
  Expression
    Identifier, CallExpression, NumberLiteral, StringLiteral, BinaryExpression
  VariableDeclaration
```

`VariableType.NUMBER` / `STRING`, parseados con `VariableType.from("number")` (case-insensitive).

`Location.empty()` = `(0,0)-(0,0)`. El `FileCodeReader` real arranca en `(1,1)`: no mezclar mundos.

El parser **no** construye estas clases. Se pueden borrar cuando el type-checker deje de usarlas; `Location` se queda (ya está en su archivo).

---

## Puertos de I/O (`reader/`)

`common` define *qué* se puede leer, no *cómo*.

```kotlin
interface CodeReader {
    fun read(): Optional<Char>       // consume
    fun peek(): Optional<Char>       // no consume
    fun currentPosition(): CharPosition
}

interface LanguageConfigReader {  // Path / InputStream / String → LanguageConfig
interface GrammarConfigReader {   // Path / InputStream / String → Grammar
```

Implementaciones: `infrastructure` (`FileCodeReader`, `JSON*ConfigReader`). Tests del lexer tienen `MockReader`.

`CharPosition(line, col)` — enteros. Convención de archivo real: 1-based. Tests del lexer: línea 0.

---

## Invariantes

- Cero dependencias de otros proyectos del repo.
- Cero `@Serializable`. Si necesitás JSON, el serializer va a `infrastructure`.
- `Grammar` es inválida si hay refs colgantes: no “arregles” eso en el parser, arreglá la gramática.
- `Token.value` vacío (`Optional.empty()`) es el caso normal de keywords/puntuación, no un error.
- `SyntaxNode.name` de una hoja capturada es el **tipo de token** (`"ID"`), no el lexema (`"pepe"`). El lexema está en `value()`.

---

## Cómo extender

Nueva `GrammarRule`:

1. Data class en `GrammarRule.kt` + `references()`
2. Si aplica, nuevo `SeqStep` en `SeqStep.kt`
3. Serializer + handler (ver CONTEXT.md receta)

Nueva categoría de token: no hace falta tocar `common` (es un string más en `LanguageConfig`). Solo tocá `common` si cambia la *forma* de `TokenRule`.

---

## Tests

`common` no tiene source set de test. Lo cubren:

- `parser` → `GrammarTest`, `SyntaxNodeTest`
- `infrastructure` → deserialización que construye estos tipos
- `:semantic` → construye el AST tipado a mano (a migrar: ver [TYPE_CHECKER.md](TYPE_CHECKER.md))
