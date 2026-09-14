# Lexer Configuration — `language.config.v1.0.json`

Configuración declarativa del lexer. Define **categorías de tokens**, su **prioridad de evaluación** y las **reglas de matching**.

## Estructura

```json
{
  "order": ["keywords", "operators", "literals", "identifiers"],
  "config": {
    "keywords": [ /* reglas */ ],
    "operators": [ /* reglas */ ],
    "literals": [ /* reglas */ ],
    "identifiers": [ /* reglas */ ]
  }
}
```

### `order`

Array de strings. Define la **prioridad de las categorías** cuando varias reglas matchean el mismo lexema.

En el código (`RuleDrawResolver`) gana la categoría con **menor índice**: la **primera** de `order`. Se prueba keywords, si no types, si no operators, y así. `order[0]` es la más prioritaria.

El `language.config.v1.0.json` de resources y los tests (`PrintScriptLanguage`, `PsSupport`) usan el mismo orden: keywords primero, identifiers último, para que `let` no salga `ID`. Ver [LEXER.md](../modules/LEXER.md).

### `config`
Mapa de categoría → lista de reglas.

Cada regla produce un token cuando matchea.

---

## Reglas

Toda regla tiene:

| Campo      | Tipo     | Descripción |
|------------|----------|-------------|
| `type`     | `"exact"` \| `"regex"` | Estrategia de matching |
| `matcher`  | `string[]` | Patrones a evaluar |
| `token`    | `string` | Tipo de token a emitir (`KEYWORD`, `IDENTIFIER`, `STRING_LITERAL`, etc.) |
| `capture`  | `boolean` | Si `true`, el texto leído se guarda como `value` del token (`ID("pepe")`) |
| `partial`  | `string` | (solo `regex`) Regex para matching parcial |

---

### `type: "exact"`

Matching exacto de strings.

```json
{
  "type": "exact",
  "matcher": ["let"],
  "token": "LET",
  "capture": false
}
```

- El matching parcial se resuelve **automáticamente** carácter a carácter.
- No se declara `partial`.

**Ejemplo de parcial:**

`"hol` no matchea ningún keyword completo → se descarta.

`"letx` matchea `let` solo si se consume exactamente.

`"le` matchea `let` parcialmente leyendo caracter por caracter.

---

### `type: "regex"`

Matching por expresión regular.

```json
{
  "type": "regex",
  "matcher": ["^[a-zA-Z_][a-zA-Z0-9_]*"],
  "token": "IDENTIFIER",
  "capture": true,
  "partial": "^[a-zA-Z_]"
}
```

- `matcher`: regex completa (debe matchear el token entero).
- `partial`: **obligatorio**. Regex que indica si el prefijo actual puede ser el comienzo de un match válido.
- Si ningún `matcher` completo matchea, se usa `partial` para decidir si se sigue leyendo carácter a carácter.

**Ejemplo de parcial (STRING_LITERAL):**

Hay **dos** reglas `STRING_LITERAL` en el JSON de producción: dobles y simples. Mismo `token`, `partial` independiente.

```json
{
  "type": "regex",
  "matcher": ["^\"[^\"]*\""],
  "token": "STRING_LITERAL",
  "capture": true,
  "partial": "^\"[^\"]*$"
}
```

```json
{
  "type": "regex",
  "matcher": ["^'[^']*'"],
  "token": "STRING_LITERAL",
  "capture": true,
  "partial": "^'[^']*$"
}
```

`"hola` y `'hola` matchean parcialmente. El value del token **incluye** las comillas. No hay escapes.

---

## Notas de diseño

- `exact` es preferible para keywords y operadores (más rápido y predecible).
- `regex` se reserva para literales e identificadores.
- `capture: true` solo cuando el valor del token importa (identificadores, literales, números).
- El orden dentro de `order` es crítico: la categoría que tiene que ganar va **primera**.
