# Formatter Configuration

Dos archivos, dos formas. Mismo patrón que language/grammar/type-system: interface en `common`, surrogate en `infrastructure`, el formatter **no** lee el JSON/YAML.

| Archivo | Quién lo escribe | Dominio | Reader |
|---|---|---|---|
| `formatter-language.json` | el lenguaje (resource interno) | `FormatterLanguageConfig` | `JSONFormatterLanguageConfigReader` |
| `formatter-user-defaults.json` | el lenguaje (resource interno) | `FormatterRulesConfig` | `JSONFormatterRulesConfigReader` |
| `.printscript/formatter.yml` | el usuario | `FormatterRulesConfig` | `YAMLFormatterRulesConfigReader` |

El YAML de usuario es **una rule y un value** (`type` + `enabled` o `count`). Tokens (`COLON`, `println`, …) no van ahí: viven en el JSON de lenguaje.

```kotlin
val language = JSONFormatterLanguageConfigReader.read(languageStream)
val defaults = JSONFormatterRulesConfigReader.read(defaultsStream)
val user = YAMLFormatterRulesConfigReader.read(userYamlPath)
val formatter = DefaultFormatterFactory.createFromConfig(language, user, defaults, grammar, lexemes)
```

Resources reales: `infrastructure/src/main/resources/formatter-language.json` y `formatter-user-defaults.json`.

Merge de usuario: YAML pisa defaults JSON por `type`; si no está en ninguno, vale el `default` del binding.

---

## Lenguaje — `formatter-language.json`

```json
{
  "rules": [
    { "type": "space-around", "token": "OPERATOR" },
    { "type": "newline-after", "token": "SEMICOLON" },
    { "type": "space-after", "token": "LET" }
  ],
  "userBindings": [
    {
      "userType": "space-before-colon",
      "type": "space-before",
      "token": "COLON",
      "param": "enabled",
      "default": true
    }
  ]
}
```

### `rules`

Rules fijas. El YAML de usuario **no** las pisa. `type` es genérico (`space-before` / `space-after` / `space-around` / `newline-before` / `newline-after`).

| Campo | Qué es |
|---|---|
| `type` | implementación (`space-*` o `newline-*`) |
| `token` | `Token.type` (`OPERATOR`, `SEMICOLON`, `LET`, …) |
| `value` | lexema opcional (p.ej. `"println"`) |
| `previous` | `Token.type` del token previo, opcional |

### `userBindings`

Mapea el `type` del YAML de usuario a una implementación genérica.

| Campo | Qué es |
|---|---|
| `userType` | el `type` que escribe el usuario (`space-before-colon`, …) |
| `type` | implementación genérica |
| `token` / `value` / `previous` | igual que en `rules` |
| `param` | `"enabled"` o `"count"` — el value del YAML |
| `default` | boolean si `param` es `enabled`; entero si es `count` |

`userType` duplicado o `param` que no sea `enabled`/`count` → `IllegalArgumentException` al construir el dominio.

Una rule de usuario nueva que entre en `TokenSpaceRule` / `TokenNewlineRule` es **solo un binding** en este JSON. Kotlin nuevo solo si aparece un kind que esas dos no cubren.

---

## Defaults — `formatter-user-defaults.json`

Misma forma que el YAML de usuario. Application lo carga siempre; si no hay YAML, estas son las rules de usuario.

```json
{
  "rules": [
    { "type": "space-before-colon", "enabled": true },
    { "type": "space-after-colon", "enabled": true },
    { "type": "space-around-assign", "enabled": true },
    { "type": "newlines-before-println", "count": 1 }
  ]
}
```

---

## Usuario — YAML (consigna)

```yaml
rules:
  - type: space-before-colon
    enabled: true
  - type: space-after-colon
    enabled: true
  - type: space-around-assign
    enabled: true
  - type: newlines-before-println
    count: 1
```

Misma forma en JSON (`FormatterRulesConfig` / `FormatRuleSpec`). JSON y YAML comparten `FormatterRulesConfigSerializer`.

| `type` (YAML) | Value | Default (v1) |
|---|---|---|
| `space-before-colon` | `enabled` | `true` |
| `space-after-colon` | `enabled` | `true` |
| `space-around-assign` | `enabled` | `true` |
| `newlines-before-println` | `count` (0..2) | `1` |

`type` que no está en `userBindings` → `UnknownRuleType`. Poner un type genérico (`space-around`) en el YAML no pisa las rules de lenguaje.

El cap de un espacio entre tokens **no** es una rule: lo aplica el registry.
