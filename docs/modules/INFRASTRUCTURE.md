# Módulo `infrastructure`

Dependencias: `common` + `kotlinx-serialization-json` + `kaml` (YAML). Es el único módulo con el plugin `kotlin-serialization`. **No** es el CLI: no tiene `Main` ni plugin `application`.

I/O concreto: leer configs JSON/YAML y leer código desde archivo. El dominio en `common` permanece ignoto de JSON. El composition root (cargar configs + llamar use cases) vive en `:cli`.

---

## Cuándo tocarlo

- Forma del JSON o YAML / un serializer nuevo
- Validación post-load de `LanguageConfig`
- Otra fuente de caracteres (`CodeReader` para stdin, string, etc.)
- **No** para la semántica de una regla: eso es `common` + `parser`/`lexer`/`type-checker`/`formatter`
- **No** para parsear args, cargar el pipeline ni presentar resultados: eso es `:cli`

---

## Mapa de archivos

```
infrastructure/src/main/
  kotlin/printscript/infrastructure/
    reader/
      JSONLanguageConfigReader.kt
      JSONGrammarConfigReader.kt
      JSONTypeSystemConfigReader.kt
      JSONFormatterLanguageConfigReader.kt
      JSONFormatterRulesConfigReader.kt
      YAMLFormatterRulesConfigReader.kt
      JSONLinterConfigReader.kt
      FileCodeReader.kt
    serializer/config/
      JsonCodecs.kt              asJsonDecoder / asJsonEncoder
      LanguageConfigSerializer.kt
      TokenRuleSerializer.kt     type: exact | regex
      ExactRuleSerializer.kt
      RegexRuleSerializer.kt
      GrammarSerializer.kt
      GrammarRuleSerializer.kt   dispatch por clave JSON
      OrRuleSerializer.kt
      AtomRuleSerializer.kt
      SeqRuleSerializer.kt
      SeqStepSerializer.kt
      LeftRuleSerializer.kt
      RepeatRuleSerializer.kt
      TypeSystemConfigSerializer.kt
      OperationSerializer.kt
      NodeConfigSerializer.kt
      FormatterRulesConfigSerializer.kt
      FormatterLanguageConfigSerializer.kt
      LinterConfigSerializer.kt
  resources/
    language.config.json
    grammar.config.json
    type-system.config.json
    formatter-language.json
    formatter-user-defaults.json
    linter.config.json
```

Specs: [LANGUAGE_CONFIG.md](../configs/LANGUAGE_CONFIG.md), [GRAMMAR_CONFIG.md](../configs/GRAMMAR_CONFIG.md), [TYPE_SYSTEM_CONFIG.md](../configs/TYPE_SYSTEM_CONFIG.md), [FORMATTER_CONFIG.md](../configs/FORMATTER_CONFIG.md).

---

## Config del lexer — `JSONLanguageConfigReader`

Object singleton que implementa `LanguageConfigReader`:

```kotlin
JSONLanguageConfigReader.read(path | inputStream | jsonString): LanguageConfig
```

`Json { ignoreUnknownKeys = true }` + `SerializersModule` polimórfico:

- `TokenRule` → `ExactRule` vía `ExactRuleSerializer` (`@SerialName("exact")`)
- `TokenRule` → `RegexRule` vía `RegexRuleSerializer` (`@SerialName("regex")`)

El JSON discrimina con `"type": "exact" | "regex"`.

### Validación (después de decode)

- `order` no vacío
- cada entrada de `order` existe como clave de `config`
- cada categoría tiene ≥1 regla
- cada regla tiene `matcher` no vacío y `token` no blank
- `RegexRule.partial` no blank

No reordena `order`: el lexer prueba las categorías de primero a último, igual que está escrito en el JSON.

`LanguageConfig` en `common` **no** está anotado `@Serializable`. El reader usa `LanguageConfigSerializer` (surrogate) + `TokenRuleSerializer` (campo JSON `type`). Los surrogates de `ExactRule` / `RegexRule` ignoran `type` (`ignoreUnknownKeys`).

Los tests de application **no** usan este reader: arman `LanguageConfig` en código.

---

## Config del parser — `JSONGrammarConfigReader`

```kotlin
JSONGrammarConfigReader.read(path | inputStream | jsonString): Grammar
```

`decodeFromString(GrammarSerializer, jsonString)`. Al construir `Grammar`, corre la validación de referencias de `common`.

Módulo polimórfico de `GrammarRule` (por si se serializa por tipo), pero el decode real de cada regla en el mapa pasa por `GrammarRuleSerializer`, que **no** usa un campo `type`. Mira las claves del objeto:

| Clave presente | Serializer | Modelo |
|---|---|---|
| `or` | `OrRuleSerializer` | `OrRule` |
| `seq` | `SeqRuleSerializer` | `SeqRule` |
| `left` | `LeftRuleSerializer` | `LeftRule` |
| `atom` | `AtomRuleSerializer` | `AtomRule` |
| `repeat` | `RepeatRuleSerializer` | `RepeatRule` |
| otra | error `Unknown grammar rule keys` | |

Una sola clave de tipo por objeto. No mezclar `{ "or": …, "seq": … }`.

### Steps de `seq` (`SeqStepSerializer`)

| JSON | Modelo |
|---|---|
| `"LET"` (string) | `TokenStep("LET", capture=false)` |
| `{ "capture": "ID" }` | `TokenStep("ID", capture=true)` |
| `{ "rule": "expression" }` | `RuleRefStep("expression")` |

Otros shapes → error.

Los serializers de gramática son bidireccionales (serialize + deserialize). Los tests cubren sobre todo deserialize.

---

## Config del type-system — `JSONTypeSystemConfigReader`

```kotlin
JSONTypeSystemConfigReader.read(path | inputStream | jsonString): TypeSystemConfig
```

Implementa `TypeSystemConfigReader` (`common`). Surrogates: `TypeSystemConfigSerializer`, `OperationSerializer` (`commutative` default `true`), `NodeConfigSerializer` (campos opcionales). Al construir `TypeSystemConfig` corre la validación de tipos referenciados.

El type-checker **no** llama a este reader: recibe el `TypeSystemConfig` ya armado.

---

## `FileCodeReader`

`CodeReader` sobre `java.io.File(path).bufferedReader()`.

- Lookahead de un carácter (`read()` del JDK)
- `read()`: si el char es `\n`, línea++ y col=1; si no, col++
- Posición inicial `(1, 1)` — **1-based**
- `peek()` no avanza posición
- No cierra el reader (no hay `Closeable`). Para archivos cortos de la materia alcanza; para un CLI largo habría que cerrar

El CLI (`PrintScriptCli`) y los tests de application le pasan un **path de filesystem**, no un classpath. Los `.ps` de test se resuelven con `File(url.toURI()).absolutePath`.

---

## Resources actuales (v1)

### `language.config.json`

Categorías: `keywords`, `types`, `operators`, `literals`, `identifiers`.

Tokens: `LET`, `CALL` (`println`, capture), `TYPE` (`string`/`number`, capture), puntuación sin capture, `OPERATOR` (`+ - * /`, capture), `STRING_LITERAL`, `NUMBER_LITERAL`, `ID`.

`order` en el archivo: keywords → types → operators → literals → identifiers. El lexer prueba en ese orden (primero gana). Ver [LEXER.md](LEXER.md).

`COMMA` está; la gramática no lo usa.

Partial de string en este JSON: `"^\"[^\"]*$"` (permite tokenizar `"hola"`). Números siguen con `^[0-9]` (el `1.5` se parte).

### `grammar.config.json`

`start: statement`. Producciones: `statement`, `variable`, `expression-stmt`, `expression`, `term`, `factor`, `number`, `string`, `identifier`, `call`, `group`. Sin `repeat`. `call` tiene un solo argumento.

### `type-system.config.json`

`types`: `number`, `string`. Literales `NUMBER_LITERAL` / `STRING_LITERAL`. Operaciones `+ - * /` (el `+` también string+string y string+number). `nodes` con kinds `declaration`, `expression`, `binary-or-primary`, `primary`, `call`, `group`, `literal`, `identifier`.

---

## `order`

JSON, docs y tests coinciden: **primero gana**. Keywords antes que identifiers para que `let` sea `LET`.

---

## Tests

`JSONGrammarConfigReaderTest`:

- Lee el resource real: `start`, `or`, `seq` (capturas + rule refs), `left`+valores, `atom`
- JSON chico con `repeat`
- Rechaza start desconocido, ref colgante, shape desconocido

`JSONTypeSystemConfigReaderTest`: resource canónico, `commutative: false`, rechaza tipos inexistentes.

`FormatterLanguageConfigReaderTest`: resource `formatter-language.json` (`rules` + `userBindings`).

`FormatterRulesConfigReaderTest`: YAML de usuario (`enabled` / `count`) + resource `formatter-user-defaults.json`. JSON y YAML de usuario comparten `FormatterRulesConfigSerializer`.

`JSONLanguageConfigReaderTest` lee el resource real (`order`, LET, STRING_LITERAL). El pipeline del CLI (`run` / `typecheck` / `format` / `check` + parse inválido → `ERROR`) vive en `PrintScriptCliTest` (`:cli`). No hay test de `FileCodeReader`.

---

## Cómo extender

Nueva `GrammarRule`:

1. Surrogate `@Serializable` con la clave JSON como único campo relevante (mirá `AtomRuleSurrogate(val atom: String)`).
2. `object XxxSerializer : KSerializer<Xxx>` igual que los otros.
3. Rama en `GrammarRuleSerializer.select(JsonElement)` y `serializerFor(rule)`.
4. `subclass` en `grammarModule()` de `JSONGrammarConfigReader`.
5. Test en `JSONGrammarConfigReaderTest`.

Nueva `TokenRule`: igual, con `@SerialName("…")` porque el JSON de tokens **sí** usa `type`.

Nuevo `CodeReader` (string in-memory, stdin): implementá la interface de `common`. El lexer no debería enterarse.
