# Parser Configuration — `grammar.config.v1.0.json`

Configuración declarativa del parser. Define la **regla de arranque** y las **producciones** que el evaluador aplica sobre tokens genéricos (`Token.type: String`).

Se carga igual que `language.config.v1.0.json`: interface en `common`, serializers en `infrastructure`.

```kotlin
val grammar = JSONGrammarConfigReader.read(Path.of("grammar.config.v1.0.json"))
val parser = DefaultParserFactory.create(grammar)
```

## Estructura

```json
{
  "start": "statement",
  "rules": {
    "statement": { "or": ["variable", "expression-stmt"] },
    "number": { "atom": "NUMBER_LITERAL" }
  }
}
```

### `start`

Nombre de la regla que evalúa cada `parseNextStatement`.

### `rules`

Mapa nombre → definición. Cada definición usa **una** clave de tipo (`or`, `seq`, `left`, `atom`, `repeat`).

---

## Tipos de regla

| JSON | Modelo | Serializer |
|---|---|---|
| `{ "or": ["a", "b"] }` | `OrRule` | `OrRuleSerializer` |
| `{ "atom": "NUMBER_LITERAL" }` | `AtomRule` | `AtomRuleSerializer` |
| `{ "seq": [ ... ] }` | `SeqRule` | `SeqRuleSerializer` |
| `{ "left": "term", "op": { "token": "OPERATOR", "values": ["+","-"] } }` | `LeftRule` | `LeftRuleSerializer` |
| `{ "repeat": "statement" }` | `RepeatRule` | `RepeatRuleSerializer` |

Los tipos de token (`LET`, `ID`, `OPERATOR`, …) son strings y tienen que coincidir con `language.config.v1.0.json`.

### Steps de `seq`

| JSON | Modelo |
|---|---|
| `"LET"` | `TokenStep("LET", capture=false)` — se consume y se tira |
| `{ "capture": "ID" }` | `TokenStep("ID", capture=true)` — queda en el árbol |
| `{ "rule": "expression" }` | `RuleRefStep("expression")` — se evalúa esa regla |

---

## Serializers

Igual que el lexer (`ExactRuleSerializer` / `RegexRuleSerializer`):

- El dominio (`Grammar`, `GrammarRule`, …) vive en `common` y **no** tiene `@Serializable`.
- Cada tipo tiene un `KSerializer` con surrogate en `infrastructure/serializer/config`.
- `GrammarRuleSerializer` elige el serializer según la clave JSON (`or`, `seq`, `left`, `atom`, `repeat`). No hay campo `type`.
- `JSONGrammarConfigReader` implementa `GrammarConfigReader` y registra los serializers en un `SerializersModule` polimórfico.

Para agregar un tipo de regla: data class en `common` + serializer + registrarlo en el module + handler en el parser.

---

## Bloques (listo, no usado en v1)

`repeat` ya se deserializa y el parser lo evalúa. Ejemplo para `if` (no está en el JSON actual):

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

---

## Árbol de salida

El parser produce `SyntaxNode` / `SyntaxProgram` (en `common`). El type-checker camina ese árbol con `child`, `find` y `value`.

## Qué no hace

- Chequeo de tipos (eso es el type-checker sobre `SyntaxNode`; ver [TYPE_SYSTEM_CONFIG.md](TYPE_SYSTEM_CONFIG.md)).
