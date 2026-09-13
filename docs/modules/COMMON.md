# Módulo `common`

Dependencias Gradle: ninguna. Todos los demás módulos dependen de este.

Contrato compartido del lenguaje: tokens, gramática, árboles, posiciones, y **puertos** de I/O (interfaces). No hay implementación de lexer, parser, JSON ni filesystem. No uses kotlinx.serialization acá.

Si un tipo lo necesitan dos módulos, vive acá. Si un tipo es detalle de matching o de un handler, no.

---

## Cuándo tocarlo

- Nueva forma de regla de gramática o de token (`GrammarRule`, `TokenRule`, `SeqStep`)
- Cambiar la forma de `SyntaxNode` / `Token` / `Location`
- Nuevo puerto (`CodeReader`, config readers, `FormatterRulesConfigReader`, `FormatterLanguageConfigReader`)
- Forma de `TypeSystemConfig` / `FormatterLanguageConfig` / `Result` / `Report`
- **No** para serializers, matching de regex, ni el walk de tipos (eso es `:type-checker`)

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
    TypeSystemConfig.kt   types, literals, operations, nodes — valida refs de tipos
    FormatterRulesConfig.kt  YAML de usuario (type + enabled/count)
    FormatterLanguageConfig.kt  JSON de lenguaje: rules fijas + userBindings
    LinterConfig.kt       rules + enabled()
    TokenLexemes.kt       ExactRule de un matcher → lexema
  syntax/
    SyntaxNode.kt         árbol genérico de salida del parser
    SyntaxProgram.kt      lista de statements + location
  ast/
    Location.kt           start/end CharPosition
  error/
    Error.kt              sealed raíz
    TypeError.kt          sealed con variantes (el checker usa otro TypeError, ver TYPE_CHECKER.md)
    RuntimeError.kt       sealed del interpreter (DivisionByZero, InvalidLiteral, …)
    FormatError.kt        sealed del formatter (MissingLexeme, UnrecognizedFormatNode, WhitespaceMismatch, …)
    LexerError.kt         sealed del lexer (UnexpectedToken, UnexpectedEnfOfLine, …)
    ParserError.kt        MissingToken / UnexpectedStart
    LintError.kt          InvalidIdentifierFormat / InvalidPrintlnArgument (implementa Error)
  SideEffect.kt           PrintEffect — output observable del interpreter
  reader/
    CodeReader.kt         puerto: read / peek / currentPosition
    CharPosition.kt       (line, col)
    LanguageConfigReader.kt
    GrammarConfigReader.kt
    TypeSystemConfigReader.kt
    FormatterRulesConfigReader.kt
    FormatterLanguageConfigReader.kt
    LinterConfigReader.kt
  util/
    Result.kt             Result.Ok/Err + Report + map/flatMap/fold/toReport/isOk
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
    val order: List<String>,                       // categorías, primero = más prioritaria
    val config: Map<String, List<TokenRule>>       // nombre de categoría → reglas
)
```

`order` significa “primero gana”. `RuleDrawResolver` (lexer) usa `order.indexOf(category)` y se queda con el **mínimo**. Ver [LEXER.md](LEXER.md).

### `FormatterRulesConfig`

Lista de `{ type, enabled?, count? }` — forma del YAML de usuario (consigna). Serializer en `infrastructure`. Ver [FORMATTER.md](FORMATTER.md) y [FORMATTER_CONFIG.md](../configs/FORMATTER_CONFIG.md).

### `FormatterLanguageConfig`

JSON de lenguaje: `rules` (type genérico + `token` / `value` / `previous`) y `userBindings` (`userType` del YAML → implementación). `userType` duplicado o `param` inválido → `IllegalArgumentException`. Ver [FORMATTER_CONFIG.md](../configs/FORMATTER_CONFIG.md).

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

Un `let` es un nodo `name = "variable"` cuyos hijos salen de los steps capturados y las reglas anidadas.

`Location.empty()` = `(0,0)-(0,0)`. El `FileCodeReader` real arranca en `(1,1)`: no mezclar mundos.

---

## Type-system (`domain/TypeSystemConfig.kt`)

Modelo de `type-system.config.json`, sin JSON. Al construirse exige que los tipos de `literals` y `operations` existan en `types`.

```kotlin
TypeSystemConfig(types, literals, operations, nodes)
Operation(op, operands, result, commutative = true)
NodeConfig(kind, id?, declaredType?, expression?, callee?, args)
```

El type-checker recibe este objeto ya armado. Spec del JSON: [TYPE_SYSTEM_CONFIG.md](../configs/TYPE_SYSTEM_CONFIG.md). Walk: [TYPE_CHECKER.md](TYPE_CHECKER.md).

---

## Result / Report (`util/Result.kt`)

`Result.Ok` / `Result.Err` + `map` / `flatMap` / `fold` / `toReport` / `isOk`. `Report(value, errors)` con `isOk` si no hay errores. Lo usan type-checker y tests.

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
interface TypeSystemConfigReader {  // Path / InputStream / String → TypeSystemConfig
interface FormatterRulesConfigReader
interface FormatterLanguageConfigReader
interface LinterConfigReader
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

Viven en `common/src/test/kotlin`, `kotlin.test`, nombres en inglés entre backticks.

- `Result` / `Report`: `isOk`, `map`, `flatMap`, `fold`, `toReport`
- `Grammar` / `GrammarRule` / `SeqStep`: start y refs, `rule()`, `references()`
- `SyntaxNode` / `SyntaxProgram` / `Location.empty`
- `TokenLexemes.from` (exacto de un matcher vs varios vs regex)
- `FormatterLanguageConfig` / `TypeSystemConfig` validación
- `LinterConfig.enabled` / `FormatterRulesConfig` defaults
- sealed errors (`TypeError`, `RuntimeError`, `LexerError`, `ParserError`, `FormatError`, `LintError`) y `PrintEffect`
- `TokenType` leftover: solo que el enum sigue existiendo

Los puertos `CodeReader` / `*ConfigReader` no tienen tests acá: son interfaces sin implementación. `:parser` sigue teniendo `GrammarTest` / `SyntaxNodeTest` de integración con el parser; `:infrastructure` cubre la deserialización.
