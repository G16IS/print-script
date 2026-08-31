# Módulo `formatter`

Dependencias: `common`. Tests tiran de `infrastructure` para cargar `formatter-language.json`.

Pretty-printer de PrintScript sobre `SyntaxProgram`. No es linter (el linter reporta; este reescribe o chequea whitespace). No ejecuta. Application lo llama desde `FormatCode` / `CheckFormat`.

Pretty-printer dirigido por rules. JSON de lenguaje (fijas) + YAML de usuario (configurables, con defaults si falta el archivo). Reconstruye la puntuación que el parser no deja en el árbol (`let`, `:`, `=`, `;`, parens).

---

## Cuándo tocarlo

- Nueva rule de estilo → `FormatRule` + `FormatRuleFactory` + entrada en `FormatRuleFactories` + JSON o YAML
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
        language: FormatterRulesConfig,
        user: FormatterRulesConfig = FormatterRulesConfig(),
        grammar: Grammar,
        lexemes: TokenLexemes,
    ): Result<Formatter, FormatError>
}
```

Siempre crear por la factory. La impl es `DefaultFormatter` + `DefaultRuleRegistry`.

- `format` recorre el árbol, emite lexemas capturados e inyecta whitespace de las rules. Fail-fast en errores estructurales (`MissingLexeme`, `UnrecognizedNode`).
- `check` usa el **source original** (el AST no tiene trivia). En cada hueco entre tokens compara el texto real vs `registry.whitespaceFor(point)` y **acumula** todos los `WhitespaceMismatch` en un `Report`. No corta en el primero.

`FormatError` es sealed **de este módulo** (`message` + `location`).

---

## Constantes de lenguaje

`FormatterConfig` (no es YAML de usuario). Solo paths de config; **no** hay knobs de pipeline (type-check, CLI, etc.):

| Constante | Default | Para qué |
|---|---|---|
| `USER_YAML_PATH` | `.printscript/formatter.yml` | Path del YAML de usuario; se cambia acá |
| `LANGUAGE_JSON_RESOURCE` | `formatter-language.json` | Resource de reglas fijas |

---

## Strategy

```kotlin
interface FormatRule {
    fun applies(point: FormatPoint): Boolean
    fun addChar(point: FormatPoint, whitespace: Char): Int
}

interface FormatRuleFactory {
    val type: String
    val userConfigurable: Boolean
    fun create(params: Map<String, Any?>): Result<FormatRule, FormatError>
}
```

El walker imprime lexemas. Las rules no devuelven un `String` libre: el registry pregunta `addChar(point, ' ')` y `addChar(point, '\n')` y **combina** (máximo de newlines, como mucho un espacio). El cap de un espacio es invariante del combiner, no una rule (la consigna no lo configura). Si nadie aplica → `""`.

El core es **inmutable**: `WalkState` es un `data class` (`output`, `errors`, `last`). `emit` / `FormatRuleLoader.instantiate` son `fold` + `copy`; no hay `StringBuilder` ni listas mutables.

El hueco entre dos tokens es `AFTER` del anterior + `BEFORE` del actual. Al terminar el programa se emite el `AFTER` del último token (newline tras `;`).

Puntuación que el parser no deja en el árbol (`let`, `:`, `=`, `;`, parens): `GrammarWalker` la reinyecta leyendo `SeqRule` + `TokenLexemes` (lexemas exactos de un solo matcher). Nodos que no son `SeqRule` (`Or`, `Left`, `Atom`, `Repeat`) caen al walk genérico de hijos.

### Rules

**Lenguaje** (`formatter-language.json`, el usuario no las overridea):

| `type` | Qué hace |
|---|---|
| `space-around-operator` | un espacio antes y después de `OPERATOR` |
| `newline-after-semicolon` | `\n` después de `;` |
| `space-after-let` | un espacio después de `let` |

**Usuario** (YAML; si el archivo no existe o falta la rule, defaults):

| `type` | Default | Qué hace |
|---|---|---|
| `space-before-colon` | `enabled: true` | espacio antes de `:` |
| `space-after-colon` | `enabled: true` | espacio después de `:` |
| `space-around-assign` | `enabled: true` | espacio alrededor de `=` |
| `newlines-before-println` | `count: 1` (0..2) | newlines extra antes de `println` si el token previo es `;` |

Ejemplo con defaults: `let x : number = 1 + 2;\n` y `1 + 2;\n\nprintln(1);\n`.

---

## Config

Misma forma semántica en JSON y YAML. Dominio en `common` (`FormatterRulesConfig` / `FormatRuleSpec`); **no** `@Serializable`. Lectura en `infrastructure`, igual que language/grammar/type-system:

- `JSONFormatterRulesConfigReader` + `FormatterRulesConfigSerializer` (surrogate)
- `YAMLFormatterRulesConfigReader` — **el mismo serializer**, vía kaml

```json
{
  "rules": [
    { "type": "space-around-operator" },
    { "type": "newline-after-semicolon" },
    { "type": "space-after-let" }
  ]
}
```

```yaml
rules:
  - type: space-before-colon
    enabled: true
  - type: newlines-before-println
    count: 1
```

`FormatRuleLoader` (en `:formatter`) no lee archivos: instancia specs ya decodificadas. `type` desconocido o type fijo (`userConfigurable == false`) en el YAML de usuario → `Result.Err` al cargar.

Resource interno: `infrastructure/src/main/resources/formatter-language.json`.

---

## Mapa de archivos

```
formatter/src/main/kotlin/printscript/formatter/
  Formatter.kt
  DefaultFormatter.kt           walk puro: fold sobre WalkState inmutable
  DefaultFormatterFactory.kt
  FormatterConfig.kt
  FormatError.kt
  FormatPoint.kt
  WhitespaceChars.kt            SPACE / NEWLINE
  RuleRegistry.kt               combina addChar: newlines + como mucho un espacio
  SourceGaps.kt                 offset CharPosition → source (para check)
  WalkState.kt
  NodeWalk.kt
  GrammarWalker.kt              SeqRule → tokens capturados / sintéticos / rule-refs
  rules/                        TokenSpaceRule + fijas + NewlinesBeforePrintlnRule
  factories/
    FormatRuleFactory.kt        lista de factories
    FactoryParams.kt            enabled / count
    *Factory.kt
  config/
    FormatRuleLoader.kt         specs → FormatRule + defaults de usuario
```

---

## Tests

Árboles fabricados (sin lex/parse):

| Clase | Qué cubre |
|---|---|
| `DefaultFormatterTest` | `1+2` → `1 + 2`; sin rules → `1+2`; `expression-stmt` + `;`; errores estructurales |
| `PrintScriptLayoutTest` | `let x : number = 1;\n`; colon sin espacios; `println` con newline extra |
| `FormatterCheckTest` | mismatches alrededor de `+` |
| `FormatRuleLoaderTest` | JSON de lenguaje; defaults de usuario; type fijo; `count` inválido |
| `SpaceAroundOperatorRuleTest` / `RuleRegistryTest` | `addChar`, combinación newline+space |

Infrastructure: `FormatterRulesConfigReaderTest` (JSON resource + YAML con `enabled`/`count`).

---

## Cómo extender

1. `FormatRule` + `FormatRuleFactory`.
2. Agregar la factory a `FormatRuleFactories.defaults()`.
3. Si es de lenguaje: entrada en `formatter-language.json`.
4. Si es de usuario: documentar params (`enabled`, `count`, …) en el surrogate del serializer.
5. Tests de `format` / `check`. La puntuación que el parser no deja en el árbol la reinyecta `GrammarWalker` desde la gramática + `TokenLexemes`.
