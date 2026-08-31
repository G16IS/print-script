# Módulo `formatter`

Dependencias: `common`. Tests tiran de `infrastructure` para cargar `formatter-language.json`.

Pretty-printer de PrintScript sobre `SyntaxProgram`. No es linter (el linter reporta; este reescribe o chequea whitespace). No ejecuta. No lee archivos: recibe configs ya parseadas. Application lo llama desde `FormatCode` / `CheckFormat`.

Pretty-printer dirigido por rules. JSON de lenguaje (rules fijas + bindings) + JSON de defaults de usuario + YAML de usuario (`type` + `enabled`/`count`, consigna). Reconstruye la puntuación que el parser no deja en el árbol (`let`, `:`, `=`, `;`, parens). Spec de las configs: [FORMATTER_CONFIG.md](../configs/FORMATTER_CONFIG.md).

---

## Cuándo tocarlo

- Nueva rule de estilo que entre en space/newline → binding o rule en `formatter-language.json`
- Nuevo *kind* de whitespace → `FormatRule` + `FormatRuleFactory` + JSON
- Cambiar el walk / `FormatPoint` / `GrammarWalker`
- Política de errores de `format` (`Result`) o `check` (`Report`)
- **No** para tokens/gramática del lenguaje
- **No** para type-check: este módulo formatea el `SyntaxProgram` que le pasen; si hay que type-chequear antes, lo decide application

---

## API pública

```kotlin
interface Formatter {
    fun format(program: SyntaxProgram): Result<String, FormatError>
    fun check(program: SyntaxProgram, source: String): Report<Unit, FormatError>
}

object DefaultFormatterFactory {
    fun create(rules: List<FormatRule>, grammar: Grammar, lexemes: TokenLexemes): Formatter
    fun createFromConfig(
        language: FormatterLanguageConfig,
        user: FormatterRulesConfig = FormatterRulesConfig(),
        defaults: FormatterRulesConfig = FormatterRulesConfig(),
        grammar: Grammar,
        lexemes: TokenLexemes,
    ): Result<Formatter, FormatError>
}
```

Siempre crear por la factory. La impl es `DefaultFormatter` + `DefaultRuleRegistry`.

- `format` recorre el árbol, emite lexemas capturados e inyecta whitespace de las rules. Fail-fast en errores estructurales (`MissingLexeme`, `UnrecognizedNode`).
- `check` usa el **source original** (el AST no tiene trivia). Recorre los mismos tokens que `format` (capturados y sintéticos). Un cursor busca cada lexema en el source y compara el hueco vs lo esperado. **No** usa `token.location.start` para el offset: el lexer deja esa posición *después* del primer carácter. Acumula todos los `WhitespaceMismatch` (también si lo esperado es `""` y hay extra, y el trailing). No corta en el primero.

`FormatError` es sealed **de este módulo** (`message` + `location`).

El módulo **no lee archivos**. Application carga JSON/YAML y pasa `FormatterLanguageConfig` + `FormatterRulesConfig` ya parseadas.

---

## Strategy

```kotlin
interface FormatRule {
    fun applies(point: FormatPoint): Boolean
    fun addChar(point: FormatPoint, whitespace: Char): Int
}

interface FormatRuleFactory {
    val types: Set<String>
    fun create(spec: ResolvedFormatRule): Result<FormatRule, FormatError>
}
```

El walker imprime lexemas. Las rules no devuelven un `String` libre: el registry pregunta `addChar(point, ' ')` y `addChar(point, '\n')` y **combina** (máximo de newlines, como mucho un espacio). El cap de un espacio es invariante del combiner, no una rule (la consigna no lo configura). Si nadie aplica → `""`.

El core es **inmutable**: `WalkState` es un `data class` (`output`, `errors`, `last`). `emit` / `FormatRuleLoader.instantiate` son `fold` + `copy`; no hay `StringBuilder` ni listas mutables.

El hueco entre dos tokens es `AFTER` del anterior + `BEFORE` del actual. Al terminar el programa se emite el `AFTER` del último token (newline tras `;`).

Puntuación que el parser no deja en el árbol (`let`, `:`, `=`, `;`, parens): `GrammarWalker` la reinyecta leyendo `SeqRule` + `TokenLexemes` (lexemas exactos de un solo matcher). Nodos que no son `SeqRule` (`Or`, `Left`, `Atom`, `Repeat`) caen al walk genérico de hijos.

### Rules

**Implementaciones** (el JSON de lenguaje elige `type` + `token`):

| `type` | Rule | Qué hace |
|---|---|---|
| `space-before` / `space-after` / `space-around` | `TokenSpaceRule` | espacio en esos puntos del `token` |
| `newline-before` / `newline-after` | `TokenNewlineRule` | N newlines (`count` 0..2); `value` / `previous` opcionales |

v1 en `rules`: `space-around`+`OPERATOR`, `newline-after`+`SEMICOLON`, `space-after`+`LET`.

**Usuario** (YAML; `type` = `userType` del binding; si el archivo no existe o falta la rule, `formatter-user-defaults.json` o el `default` del binding):

| `type` | Default | Qué hace |
|---|---|---|
| `space-before-colon` | `enabled: true` | espacio antes de `:` |
| `space-after-colon` | `enabled: true` | espacio después de `:` |
| `space-around-assign` | `enabled: true` | espacio alrededor de `=` |
| `newlines-before-println` | `count: 1` (0..2) | newlines extra antes de `println` si el token previo es `;` |

Ejemplo con defaults: `let x : number = 1 + 2;\n` y `1 + 2;\n\nprintln(1);\n`.

---

## Config

Dos formas. Lectura en `infrastructure`, dominio en `common` (**no** `@Serializable`). Detalle: [FORMATTER_CONFIG.md](../configs/FORMATTER_CONFIG.md).

- Lenguaje: `JSONFormatterLanguageConfigReader` + `FormatterLanguageConfigSerializer` (`rules` + `userBindings`)
- Usuario / defaults: `YAMLFormatterRulesConfigReader` / `JSONFormatterRulesConfigReader` + `FormatterRulesConfigSerializer` (`type` + `enabled`/`count`)

`FormatRuleLoader` no lee archivos: instancia `FormatterLanguageConfig` + user + defaults ya decodificadas. Merge: YAML pisa defaults JSON por `type`; si falta en ambos, el `default` del binding. YAML `type` que no está en `userBindings` → `UnknownRuleType`.

Resources: `formatter-language.json`, `formatter-user-defaults.json`.

---

## Mapa de archivos

```
formatter/src/main/kotlin/printscript/formatter/
  Formatter.kt
  DefaultFormatter.kt           walk puro: fold sobre WalkState inmutable
  LexemeEmit.kt                 emite lexema (format) o compara hueco (check)
  DefaultFormatterFactory.kt
  FormatError.kt
  FormatPoint.kt
  WhitespaceChars.kt            SPACE / NEWLINE
  RuleRegistry.kt               combina addChar: newlines + como mucho un espacio
  SourceGaps.kt                 offset / position CharPosition ↔ source (para check)
  WalkState.kt
  NodeWalk.kt
  GrammarWalker.kt              SeqRule → tokens capturados / sintéticos / rule-refs
  rules/                        TokenSpaceRule + TokenNewlineRule
  factories/
    FormatRuleFactory.kt        SpaceRuleFactory + NewlineRuleFactory
    ResolvedFormatRule.kt       type genérico + token + enabled/count
    SpaceRuleFactory.kt
    NewlineRuleFactory.kt
  config/
    FormatRuleLoader.kt         language + user + defaults → FormatRule
```

---

## Tests

Árboles fabricados (sin lex/parse):

| Clase | Qué cubre |
|---|---|
| `DefaultFormatterTest` | `1+2` → `1 + 2`; sin rules → `1+2`; `expression-stmt` + `;`; errores estructurales |
| `PrintScriptLayoutTest` | `let x : number = 1;\n`; colon sin espacios; `println` con newline extra |
| `FormatterCheckTest` | mismatches alrededor de `+`; `;` sintético; trailing; `let` sin espacios; extra líder |
| `FormatRuleLoaderTest` | JSON de lenguaje; defaults JSON; fallback del binding; type desconocido; `count` inválido |
| `TokenSpaceRuleTest` / `TokenNewlineRuleTest` / `RuleRegistryTest` | `addChar`, println vs otro call, combinación newline+space |

Infrastructure: `FormatterLanguageConfigReaderTest` (resource) + `FormatterRulesConfigReaderTest` (YAML + defaults JSON).

---

## Cómo extender

1. Si entra en space/newline: `rules` o `userBindings` en `formatter-language.json`. El YAML de usuario no cambia de forma (`type` + un value).
2. Si no: `FormatRule` + `FormatRuleFactory` en `FormatRuleFactories.defaults()`.
3. Tests de `format` / `check`. La puntuación que el parser no deja en el árbol la reinyecta `GrammarWalker`.
