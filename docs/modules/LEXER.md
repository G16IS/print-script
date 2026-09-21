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
    companion object {
        fun create(codeReader: CodeReader, langConfig: LanguageConfig): Lexer
    }
    fun nextToken(): Result<Token, LexerError>
    fun peek(offset: Int = 0): Result<Token, LexerError>    // no consume
}
```

Siempre crear el lexer por `Lexer.create`. La impl es `TokenStream`; `DefaultLexerFactory` es `internal`.

`ParseProgram` usa `lexer.peek()` (`Result`) y corta si `type == "EOF"` (`TokenStream.END_TOKEN`).

---

## Mapa de archivos

```
lexer/src/main/kotlin/printscript/
  Lexer.kt                  interface + companion create
  DefaultLexerFactory.kt    internal; TokenStream(reader, RuleEvaluator, RuleDrawResolver)
  TokenStream.kt            loop de lectura + buffer de peek
  RuleDrawResolver.kt       desempate por categoría → Result
  TokenFactory.kt           Token a partir de TokenRule + texto + location
  evaluator/
    RuleEvaluator.kt        despacha a MatchingRuleEvaluator por regla
    MatchingRuleEvaluator.kt
    ExactEvaluator.kt
    RegexEvaluator.kt
    MatchResult.kt
    MatchType.kt            VALID | PARTIAL | INVALID
```

---

## Algoritmo (`TokenStream.readNextToken`)

1. `skipWhitespace()` — `read()` hasta un no-whitespace o EOF.
2. Si EOF: emitir `Token("EOF", empty, Location(pos, pos))`.
3. El primer carácter no-ws ya está consumido. `text` empieza ahí.
4. Evaluar **todas** las reglas contra `text`. Si todas son `INVALID` → `Result.Err(UnexpectedToken)`.
5. Loop: `peek` del siguiente carácter.
   - Si no hay más: si hay algún `VALID`, emitir; si no, `Result.Err(UnexpectedEnfOfLine)` (caso típico: string sin cerrar).
   - Evaluar `text + nextChar`. Si **todo** `INVALID` o no-parcial: **no** consumir ese carácter; emitir token con el `text` anterior.
   - Si algo sigue `VALID` o `PARTIAL`: `read()`, agrandar `text`, repetir.
6. Emitir: filtrar solo matches `VALID` (un `PARTIAL` no gana el desempate), resolver empate, `TokenFactory.create`.

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

Strings: matcher `^"[^"]*"` , partial `^"[^"]*$` (JSON de resources y tests de application). Números: matcher `^[0-9]+(\.[0-9]+)?` ; partial `^[0-9]` (JSON, parte `1.5`) o `^[0-9]+(\.[0-9]*)?$` (tests del lexer). IDs: `^[a-zA-Z_][a-zA-Z0-9_]*` / `^[a-zA-Z_]`.

`RegexEvaluator` cachea un `Regex` por patrón (`matcher` y `partial`) en la instancia. `Lexer.create` arma un evaluator por stream: el primer carácter compila, el resto reusa. Sin cache, un archivo grande recompila `Pattern` en cada carácter y no entra en el heap de 15m del TCK.

---

## Prioridad (`RuleDrawResolver`)

Cuando el munch termina, solo compiten las reglas `VALID` (ej. `let` es keyword **e** identificador). Un `PARTIAL` sirve para seguir leyendo, no para ganar: con `let n: number`, `n` es `PARTIAL` de `TYPE` (`number`) y `VALID` de `ID`; si el parcial compitiera, `types` (antes en `order`) emitiría `TYPE`. Se prueba categoría por categoría, **primera en `order` gana**.

```
findHighestPriorityCategory = minBy { order.indexOf(category) }  // ausente → Int.MAX_VALUE
```

- Índice **más bajo** gana → la categoría **primera** en `order` gana.
- Categoría ausente de `order`: prioridad más baja (no le gana a ninguna listada).
- Si la categoría ganadora tiene **más de una** regla matching → `Result.Err(MultipleRulesWithSamePriority)`.
- Si la regla no está en `config` → `Result.Err(RuleNotFound)`.
- Si no hay reglas matching → `Result.Err(NoRulesProvided)`.

Coincide con `language.config.v1.0.json`:

```json
"order": ["keywords", "types", "operators", "literals", "identifiers"]
```

`let` es `LET` (keywords antes que identifiers). Los tests usan el mismo `order`.

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
- `MockReader` de tests: `currentPosition = (0, index)` post-incremento. Por eso `TokenStreamTest.Locations` espera `LET` en `(0,1)-(0,3)` para `"let …"`.

No compares locations de tests de lexer con las de `FileCodeReader`.

---

## Errores

`nextToken` / `peek` devuelven `Result<Token, LexerError>`. No lanzan en el camino de tokenización.

| Situación | Qué devuelve |
|---|---|
| Primer carácter no matchea nada | `Result.Err(UnexpectedToken)` |
| EOF a mitad de un parcial (ej. `"hola`) | `Result.Err(UnexpectedEnfOfLine)` |
| Empate en la misma categoría | `Result.Err(MultipleRulesWithSamePriority)` |
| Regla matching ausente del config | `Result.Err(RuleNotFound)` |
| `peek`/`next` después de EOF | `Result.Ok` con otro EOF (el parser se detiene al ver `EOF`) |

`RuleEvaluator` sí puede tirar `IllegalArgumentException` si ninguna estrategia (exact/regex) aplica a una regla.

---

## Tests

JUnit 5. Harness en `lexer/src/test/kotlin/printscript/support/`:

- `PrintScriptLanguage` — mismo `ORDER` que el JSON (keywords first) y `partial`s que dejan terminar decimales y strings. `reversedOrder()` / `PRODUCTION_*_PARTIAL` documentan qué pasa si invertís el order o usás los `partial`s estrechos del resource.
- `lexer` / `lex` / `assertLex` / `assertTypes` / `tok` — agregar un caso es una línea: `assertLex("letter", tok("ID", "letter"), tok("EOF"))`.
- `MockReader` — `CodeReader` in-memory. Locations línea 0.

Los tests **no** cargan `language.config.v1.0.json`. Van por `DefaultLexerFactory` (visible en el mismo módulo) / `Lexer.create`.

| Clase | Qué cubre |
|---|---|
| `PrintScriptLexerTest` | kinds v1 (keywords vs IDs, types, números, strings, ops, statements) y que invertir el `order` hace `let` → `ID` |
| `TokenStreamTest` | whitespace, EOF, `peek`, errores, locations, greedy / empate de prefijos, `partial`s estrechos del JSON |
| `RuleEvaluatorTest` | VALID / PARTIAL / INVALID de exact y regex; se evalúan todas las reglas; `partial`s de production |
| `RuleDrawResolverTest` | primero en `order` gana; empates; regla desconocida; prioridad PrintScript (`let` vs `ID`) |
| `TokenFactoryTest` | `capture` true/false, type, location |

Si cambiás la prioridad, actualizá `PrintScriptLanguage.ORDER` acá **y** los `PrintScriptLanguage` / `PsSupport` de application e interpreter.

---

## Cómo extender

Keyword nuevo: JSON + regla en `PrintScriptLanguage` + un caso en `PrintScriptLexerTest`. Asegurate de que su categoría quede **antes** de `identifiers` en `order` (el resolver prueba de primero a último).

Literal nuevo (regex): `matcher` completo + `partial` que acepte prefijos. Si `partial` es demasiado amplio, el lexer se come caracteres de más y después falla al emitir.

No agregues lógica “si el texto es `let` entonces LET” en Kotlin. Eso duplica el JSON.
