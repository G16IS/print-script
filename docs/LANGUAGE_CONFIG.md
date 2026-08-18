# Lexer Configuration — `language.config.json`

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
Array de strings. Define el **orden de prioridad** de las categorías.

El lexer prueba las categorías en el orden indicado. La **primera regla que matchea** gana.

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

```json
{
  "type": "regex",
  "matcher": ["^\".*?\""],
  "token": "STRING_LITERAL",
  "capture": true,
  "partial": "^\""
}
```

`"hola` matchea parcialmente porque empieza con `"`.

---

## Notas de diseño

- `exact` es preferible para keywords y operadores (más rápido y predecible).
- `regex` se reserva para literales e identificadores.
- `capture: true` solo cuando el valor del token importa (identificadores, literales, números).
- El orden dentro de `order` es crítico: reglas más específicas primero.
