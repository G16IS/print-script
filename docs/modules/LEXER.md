# Módulo `lexer`

Dependencias: `common`. Lo usan `parser` (interface `Lexer`) y `application`.

Convierte un stream de caracteres (`CodeReader`) en un stream de `Token` según un `LanguageConfig`. No sabe qué es un statement. No parsea.

---

## Cuándo tocarlo

- Cambiar el algoritmo de matching (exact/regex, parciales, whitespace)
- Empates entre reglas / prioridad de categorías
- Cómo se construye un `Token` (capture, location, EOF)
- **No** para agregar un keyword al lenguaje: eso es JSON (+ tests). El lexer ya es data-driven

---

## API pública

```kotlin
interface Lexer {
    fun nextToken(): Token
    fun peek(offset: Int?): Token    // offset null = 0; no consume
}

object DefaultLexerFactory {
    fun create(codeReader: CodeReader, langConfig: LanguageConfig): Lexer
}
```

Siempre crear el lexer por la factory. La impl es `TokenStream`.

`application` usa `lexer.peek(null).type != "EOF"` para saber si queda statement.

---

## Mapa de archivos

```
lexer/src/main/kotlin/printscript/
  Lexer.kt                  interface
  DefaultLexerFactory.kt    TokenStream(reader, RuleEvaluator, RuleDrawResolver)
  TokenStream.kt            loop de lectura + buffer de peek
  RuleDrawResolver.kt       desempate por categoría
  TokenFactory.kt           Token a partir de TokenRule + texto + location
  TokenRegistry.kt          leftover, no lo usa TokenStream
  evaluator/
    RuleEvaluator.kt        ExactRule / RegexRule → MatchType
    MatchResult.kt
    MatchType.kt            VALID | PARTIAL | INVALID
```

---

## Algoritmo (`TokenStream.readNextToken`)

1. `skipWhitespace()` — `read()` hasta un no-whitespace o EOF.
2. Si EOF: emitir `Token("EOF", empty, Location(pos, pos))`.
3. El primer carácter no-ws ya está consumido. `text` empieza ahí.
4. Evaluar **todas** las reglas contra `text`. Si todas son `INVALID` → `Error("Unexpected token at line …")`.
5. Loop: `peek` del siguiente carácter.
   - Si no hay más: si hay algún `VALID`, emitir; si no, `IllegalStateException` (caso típico: string sin cerrar).
   - Evaluar `text + nextChar`. Si **todo** `INVALID` o no-parcial: **no** consumir ese carácter; emitir token con el `text` anterior.
   - Si algo sigue `VALID` o `PARTIAL`: `read()`, agrandar `text`, repetir.
6. Emitir: filtrar matches no-`INVALID`, resolver empate, `TokenFactory.create`.

El lexer es **greedy en caracteres** (maximal munch) y **no** greedy entre reglas de la misma categoría: si dos reglas de la categoría ganadora matchean, `RuleDrawResolver` tira.

Peek del `Lexer` usa un `ArrayDeque` interno: `peek(n)` rellena hasta `n`. `nextToken()` saca del buffer o lee.

---

## Matching (`RuleEvaluator`)

Evalúa **todas** las reglas de **todas** las categorías. No corta en la primera.

### `ExactRule`

- `text` igual a algún `matcher` → `VALID`
- `text` es prefijo de algún `matcher` → `PARTIAL` (no hay campo `partial` en JSON)
- si no → `INVALID`

`let` es `VALID` para LET. `le` es `PARTIAL`. `letx` es `INVALID` para LET (no es igual ni prefijo) y `VALID`/`PARTIAL` para ID.

### `RegexRule`

- algún `matcher` (regex) `matches(text)` → `VALID`
- si no, `Regex(partial).matches(text)` → `PARTIAL`
- si no → `INVALID`

`partial` es obligatorio (lo valida `JSONLanguageConfigReader`).

Strings: matcher `^"[^"]*"` , partial `^"` (en el JSON de resources) o `^"[^"]*$` (en tests). Números: `^[0-9]+(\.[0-9]+)?` / `^[0-9]`. IDs: `^[a-zA-Z_][a-zA-Z0-9_]*` / `^[a-zA-Z_]`.

Se recompila el `Regex` en cada evaluación. No hay cache.

---

## Prioridad (`RuleDrawResolver`) — trampa importante

Cuando el munch termina, pueden quedar varias reglas `VALID` o `PARTIAL` (ej. `let` es keyword **e** identificador).

```
findHighestPriorityCategory = maxBy { order.indexOf(category) }
```

- Índice **más alto** gana → la categoría **última** en `order` gana.
- Categoría ausente de `order`: `indexOf` = `-1` → la más baja.
- Si la categoría ganadora tiene **más de una** regla matching → `IllegalArgumentException`.
- Si la regla no está en `config` → `IllegalStateException`.

`docs/LANGUAGE_CONFIG.md` dice lo contrario (“el `order` se prueba de primero a último, gana la primera”). El JSON de resources está escrito con esa convención:

```json
"order": ["keywords", "types", "operators", "literals", "identifiers"]
```

Con el resolver actual, **identifiers ganarían** y `let` saldría `ID`.

Los tests **invierten** el order:

```kotlin
listOf("identifiers", "literals", "operators", "types", "keywords")
```

Eso está en `lexer/.../MockLexerFactory` y `application/.../PrintScriptLanguage`. El comentario en `PrintScriptLanguage` lo explica.

Si cableás `JSONLanguageConfigReader` + el resource tal cual, rompés keywords. O se invierte el JSON, o se cambia el resolver para que `min` índice gane, o se invierte al cargar. Hoy no está unificado.

---

## Construcción del token (`TokenFactory`)

```kotlin
Token(
    type = rule.token,                                    // string del JSON
    value = if (rule.capture) Optional.of(text) else empty,
    location = Location(initialPos, finalPos)
)
```

- `capture: false` → `LET`, `;`, `(`, `=` — el parser los reconoce por `type`
- `capture: true` → `ID`, literales, `TYPE`, `OPERATOR`, `CALL` (`"println"`)
- El value de un string **incluye las comillas**: `"hola"` no `hola`

EOF no pasa por `TokenFactory`; lo arma `TokenStream` a mano.

---

## Locations

`initialPos` se toma **después** de haber leído el primer carácter no-ws, usando `reader.currentPosition()`.

- `FileCodeReader`: al hacer `read()`, avanza col/línea **después** de devolver el char. Posiciones 1-based.
- `MockReader` de tests: `currentPosition = (0, index)` post-incremento. Por eso `TokenizeTest` espera `LET` en `(0,1)-(0,3)` para `"let …"`.

No compares locations de tests de lexer con las de `FileCodeReader`.

---

## Errores

| Situación | Qué tira |
|---|---|
| Primer carácter no matchea nada | `Error("Unexpected token at line …")` |
| EOF a mitad de un parcial (ej. `"hola`) | `IllegalStateException("Unexpected end of file…")` |
| Empate en la misma categoría | `IllegalArgumentException` desde el resolver |
| `peek`/`next` después de EOF | vuelve a emitir EOF (no avanza más allá: cada `readNextToken` al final del archivo produce otro EOF si se llama de nuevo; el parser se detiene al ver `EOF`) |

No hay tipo `LexException`. Si unificás errores, este módulo es el más informal.

---

## Leftover

`TokenRegistry`: mapa `String → (Location) → Token`. No está en el camino de `TokenStream`. No lo extiendas; el camino vivo es `TokenFactory` + `TokenRule.token`.

---

## Tests

| Clase | Qué cubre |
|---|---|
| `TokenizeTest` | `let x: string = "hello";` y `println(name + " " + lastName);`; string sin cerrar |
| `RuleDrawResolverTest` | último en `order` gana; empates; regla desconocida |
| `MockLexerFactory` / `MockReader` / `TokenLister` | harness |

`MockLexerFactory.createOrder()` ya trae el order invertido. Si cambiás la prioridad, actualizá factory **y** `PrintScriptLanguage`.

---

## Cómo extender

Keyword nuevo: JSON + tests. Asegurate de que su categoría quede **después** de `identifiers` en el `order` que realmente usa el resolver.

Literal nuevo (regex): `matcher` completo + `partial` que acepte prefijos. Si `partial` es demasiado amplio, el lexer se come caracteres de más y después falla al emitir.

No agregues lógica “si el texto es `let` entonces LET” en Kotlin. Eso duplica el JSON.
