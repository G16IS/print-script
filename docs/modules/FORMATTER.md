# Módulo `formatter`

Dependencias: `common`. Tests tiran de `infrastructure` para cargar `formatter-language.json`.

Pretty-printer de PrintScript sobre `SyntaxProgram`. No es linter (el linter reporta; este reescribe o chequea whitespace). No ejecuta. **No está cableado** en `application`.

Hoy es el **core**: una sola rule implementada (`space-around-operator`), Strategy + registry, `format` / `check`, configs JSON (lenguaje) y YAML (usuario) leídas con los serializers de `infrastructure`. No reconstruye tokens que el parser no capturó (`LET`, `:`, `=`, `;`).

---

## Cuándo tocarlo

- Nueva rule de estilo → `FormatRule` + `FormatRuleFactory` + entrada en `FormatRuleFactories` + JSON o YAML
- Cambiar el walk / `FormatPoint`
- Política de errores de `format` (`Result`) o `check` (`Report`)
- **No** para tokens/gramática del lenguaje
- **No** para type-check (ver `FormatterConfig.REQUIRES_TYPE_CHECK`)

---

## API pública

```kotlin
interface Formatter {
    fun format(program: SyntaxProgram): Result<String, FormatError>
    fun check(program: SyntaxProgram, source: String): Report<Unit, FormatError>
}

object DefaultFormatterFactory {
    fun create(rules: List<FormatRule>): Formatter
    fun createFromConfig(
        language: FormatterRulesConfig,
        user: FormatterRulesConfig = FormatterRulesConfig(),
    ): Result<Formatter, FormatError>
}
```

Siempre crear por la factory. La impl es `DefaultFormatter` + `DefaultRuleRegistry`.

- `format` recorre el árbol, emite lexemas capturados e inyecta whitespace de las rules. Fail-fast en errores estructurales (`MissingLexeme`, `UnrecognizedNode`).
- `check` usa el **source original** (el AST no tiene trivia). En cada hueco entre tokens compara el texto real vs `registry.whitespaceFor(point)` y **acumula** todos los `WhitespaceMismatch` en un `Report`. No corta en el primero.

`FormatError` es sealed **de este módulo** (`message` + `location`).

---

## Constantes de lenguaje

`FormatterConfig` (no es YAML de usuario):

| Constante | Default | Para qué |
|---|---|---|
| `REQUIRES_TYPE_CHECK` | `false` | El use-case/CLI (cuando exista) puede saltear el type-checker |
| `USER_YAML_PATH` | `.printscript/formatter.yml` | Path del YAML de usuario; se cambia acá |
| `LANGUAGE_JSON_RESOURCE` | `formatter-language.json` | Resource de reglas fijas |

---

## Strategy

```kotlin
interface FormatRule {
    fun applies(point: FormatPoint): Boolean
    fun whitespace(point: FormatPoint): String
}

interface FormatRuleFactory {
    val type: String
    val userConfigurable: Boolean
    fun create(params: Map<String, Any?>): Result<FormatRule, FormatError>
}
```

El walker imprime lexemas. Las rules solo contestan whitespace en un `FormatPoint` (`BEFORE_TOKEN` / `AFTER_TOKEN`). Si nadie aplica → `""`.

El hueco entre dos tokens es `AFTER` del anterior + `BEFORE` del actual. `space-around-operator` aplica a `tokenType == "OPERATOR"` y devuelve `" "`.

---

## Config

Misma forma semántica en JSON y YAML. Dominio en `common` (`FormatterRulesConfig` / `FormatRuleSpec`); **no** `@Serializable`. Lectura en `infrastructure`, igual que language/grammar/type-system:

- `JSONFormatterRulesConfigReader` + `FormatterRulesConfigSerializer` (surrogate)
- `YAMLFormatterRulesConfigReader` — **el mismo serializer**, vía kaml

```json
{ "rules": [ { "type": "space-around-operator" } ] }
```

```yaml
rules: []
```

`FormatRuleLoader` (en `:formatter`) no lee archivos: instancia specs ya decodificadas. `type` desconocido o type fijo (`userConfigurable == false`) en el YAML de usuario → `Result.Err` al cargar.

Resource interno: `infrastructure/src/main/resources/formatter-language.json`.

---

## Mapa de archivos

```
formatter/src/main/kotlin/printscript/formatter/
  Formatter.kt
  DefaultFormatter.kt
  DefaultFormatterFactory.kt
  FormatterConfig.kt
  FormatError.kt
  FormatPoint.kt
  RuleRegistry.kt
  SourceGaps.kt                 offset CharPosition → source (para check)
  rules/
    FormatRule.kt
    SpaceAroundOperatorRule.kt
  factories/
    FormatRuleFactory.kt
    SpaceAroundOperatorFactory.kt
  config/
    FormatRuleLoader.kt         specs → FormatRule (sin I/O)
```

---

## Limitación actual (árbol)

El parser descarta tokens sin `capture`. Un `let` no trae `LET`/`: `/`;` en el árbol. El core solo formatea lo que sí está (literales, ids, `OPERATOR`). Reconstruir keyword/puntuación es el siguiente corte.

---

## Tests

Árboles fabricados (sin lex/parse):

| Clase | Qué cubre |
|---|---|
| `DefaultFormatterTest` | `1+2` → `1 + 2`; sin rules → `1+2`; anidado `1+2*3`; `Result.Err` estructural |
| `FormatterCheckTest` | `Report` con **dos** mismatches en `1+2`; ok en `1 + 2`; acumula un lado en `1+ 2` |
| `FormatRuleLoaderTest` | JSON real vía reader; type desconocido; type fijo en user |
| `SpaceAroundOperatorRuleTest` / `RuleRegistryTest` | `applies` y concat de whitespace |

Infrastructure: `FormatterRulesConfigReaderTest` (JSON resource + YAML con `enabled`/`count`).

---

## Cómo extender

1. `FormatRule` + `FormatRuleFactory`.
2. Agregar la factory a `FormatRuleFactories.defaults()`.
3. Si es de lenguaje: entrada en `formatter-language.json`.
4. Si es de usuario: documentar params (`enabled`, `count`, …) en el surrogate del serializer.
5. Tests de `format` y `check` sobre un árbol chico. El walker no se toca.
