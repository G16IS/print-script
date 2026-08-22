# PrintScript G16 — contexto para un agente

Este archivo es el **punto de entrada**. Léelo entero antes de tocar código. Después abrí solo el `.md` del módulo que vas a cambiar.

Monorepo Gradle (`print-script-g16`) de un intérprete/analizador de **PrintScript**: lenguaje de script de la materia (declaraciones `let`, `println`, expresiones aritméticas y concatenación). Stack: **Kotlin 2.4 / JVM 21**. No hay frontend ni servicios HTTP.

El diseño es **pipeline + configuración declarativa**. Lexer y parser no hardcodean el lenguaje: lo leen de JSON. El dominio (tokens, gramática, árbol genérico) vive en `common` y **no** depende de kotlinx.serialization.

---

## Índice de documentación

### Este archivo

| Archivo | Qué cubre |
|---|---|
| [CONTEXT.md](CONTEXT.md) | Visión del proyecto, pipeline, módulos, invariantes, huecos, cómo extender |

### Módulos implementados

| Módulo | Archivo | Una línea |
|---|---|---|
| `common` | [modules/COMMON.md](modules/COMMON.md) | Tipos compartidos: Token, Grammar, SyntaxNode, AST tipado, readers |
| `lexer` | [modules/LEXER.md](modules/LEXER.md) | Código → tokens, matching exact/regex, prioridad por categoría |
| `parser` | [modules/PARSER.md](modules/PARSER.md) | Tokens → `SyntaxProgram` evaluando `Grammar` |
| `infrastructure` | [modules/INFRASTRUCTURE.md](modules/INFRASTRUCTURE.md) | JSON + filesystem: configs y `FileCodeReader` |
| `application` | [modules/APPLICATION.md](modules/APPLICATION.md) | Caso de uso `interpretCode` que arma el pipeline |

### Módulos a futuro (pipeline)

| Módulo | Archivo | Una línea |
|---|---|---|
| `type-checker` | [modules/TYPE_CHECKER.md](modules/TYPE_CHECKER.md) | Tipos y símbolos sobre el parse tree. Código hoy en Gradle `:semantic` |
| `interpreter` | [modules/INTERPRETER.md](modules/INTERPRETER.md) | Ejecutar el programa. Vacío — a futuro |
| `linter` | [modules/LINTER.md](modules/LINTER.md) | Reglas de estilo / análisis estático. Vacío — a futuro |
| `formatter` | [modules/FORMATTER.md](modules/FORMATTER.md) | Reescribir el código con estilo canónico. Vacío — a futuro |

### Build (no es pipeline)

| Módulo | Archivo | Una línea |
|---|---|---|
| `build-logic` | [modules/BUILD_LOGIC.md](modules/BUILD_LOGIC.md) | Included build: convention plugin `printscript.quality` (ktlint + detekt) |

### Configuración del lenguaje (ya existían)

| Archivo | Qué cubre |
|---|---|
| [LANGUAGE_CONFIG.md](LANGUAGE_CONFIG.md) | Forma de `language.config.json` (reglas exact/regex del lexer) |
| [GRAMMAR_CONFIG.md](GRAMMAR_CONFIG.md) | Forma de `grammar.config.json` (producciones del parser) |

Los JSON reales están en `infrastructure/src/main/resources/`.

---

## Qué es este proyecto

PrintScript v1 (el que implementa el JSON actual):

```
let pepe: string = "Hello, World!";
let pepa: number = 42;
println(pepe);
println(1 + 2 * 3);
```

Soportado hoy:

- Declaración: `let <id>: <string|number> = <expr>;`
- Statement de expresión: `<expr>;` (incluye `println(<expr>);`)
- Expresiones: literales number/string, identificadores, `+ - * /`, agrupación `( )`
- Precedencia: `* /` sobre `+ -`; asociatividad izquierda
- `+` de strings es concatenación **a nivel semántico** (el parser no distingue)

No está en la gramática v1 (pero el parser ya sabe evaluar `repeat`, pensado para bloques/`if`):

- `if`, braces, asignaciones sueltas, múltiples argumentos en `println`

Hoy el pipeline **corta en el parser**: lexea + parsea y devuelve el árbol. El type-checker vive como Gradle `:semantic` y **no está cableado**. Interpreter, linter y formatter no existen.

---

## Pipeline

```
archivo .ps
    │
    ▼
FileCodeReader          infrastructure     CodeReader carácter a carácter
    │
    ▼
TokenStream / Lexer     lexer              Token(type: String, value, location)
    │                                      reglas de language.config.json
    ▼
DefaultParser           parser             SyntaxNode / SyntaxProgram
    │                                      reglas de grammar.config.json
    ▼
type-checker            a futuro           validar tipos / símbolos
    │                                      (código actual: Gradle :semantic, desconectado)
    ▼
interpreter             a futuro           ejecutar (println, asignaciones, …)
```

Linter y formatter no están en esa cadena: corren sobre el árbol (post-parser, en paralelo o después del type-checker). Ver [modules/LINTER.md](modules/LINTER.md) y [modules/FORMATTER.md](modules/FORMATTER.md).

Armado típico (lo que hace `interpretCode`):

```kotlin
val codeReader = FileCodeReader(path)
val lexer = DefaultLexerFactory.create(codeReader, langConfig)
val parser = DefaultParserFactory.create(grammar)

var program = SyntaxProgram.empty()
while (lexer.peek(null).type != "EOF") {
    program = parser.parseNextStatement(lexer, program)
}
```

El parser es **streaming por statement**: no parsea el archivo de una. Cada llamada consume tokens hasta terminar un `statement` (regla `start` de la gramática).

---

## Mapa de módulos Gradle

```
settings.gradle.kts incluye:
  common, lexer, infrastructure, semantic, parser, application
  pluginManagement { includeBuild("build-logic") }  — convention plugin, no es library
```

Dependencias de **producción**:

```
common          ← nadie (solo JDK)
lexer           ← common
parser          ← common, lexer
semantic        ← common
infrastructure  ← common   (+ kotlinx.serialization)
application     ← common, lexer, parser, semantic, infrastructure
```

Dependencias extra de **test**:

- `parser` testImplementation `infrastructure` (carga `grammar.config.json`)
- `application` tests usan `JSONGrammarConfigReader` + un `LanguageConfig` armado en código (`PrintScriptLanguage`), no el JSON del lexer tal cual

Toolchain: `kotlin.jvmToolchain(21)`. Version catalog: `gradle/libs.versions.toml`.

Paquetes:

- Todo lo reutilizable: `printscript.*`
- Application: `edu.austral.dissis` / `edu.austral.dissis.usecases`

---

## Resumen de cada módulo

### `common` — contrato compartido

Única fuente de tipos. **No** tiene serializers ni I/O concreto.

- **Léxico:** `Token` (`type` es `String`, no el enum), `TokenRule` (`ExactRule` / `RegexRule`), `LanguageConfig`
- **Gramática:** `Grammar` (valida start + referencias al construirse), `GrammarRule` (`Or`, `Atom`, `Seq`, `Left`, `Repeat`), `SeqStep` (`TokenStep`, `RuleRefStep`), `OperatorSpec`
- **Árbol que produce el parser:** `SyntaxNode` / `SyntaxProgram` (nombres = reglas de la gramática)
- **Árbol tipado leftover:** `ast/ASTDefinition.kt` (`Program`, `VariableStatement`, …). Lo usa **solo** Gradle `:semantic` (a migrar a type-checker sobre `SyntaxNode`)
- **Puertos:** `CodeReader`, `LanguageConfigReader`, `GrammarConfigReader`
- **Ubicación:** `Location` + `CharPosition`

Ver [modules/COMMON.md](modules/COMMON.md).

### `lexer` — código a tokens

`TokenStream` lee caracteres, salta whitespace, y agranda el lexema mientras alguna regla sea `VALID` o `PARTIAL`. Cuando el próximo carácter deja todo `INVALID`, emite el token del match previo.

- Matching: `RuleEvaluator` (exact = igualdad/prefijo; regex = `matcher` + `partial`)
- Empate entre categorías: `RuleDrawResolver` — **gana la categoría con mayor índice en `LanguageConfig.order`** (la **última** de la lista)
- Factory: `DefaultLexerFactory.create(codeReader, langConfig)`
- `Token.type` es el string `token` de la regla (`"LET"`, `"ID"`, `"NUMBER_LITERAL"`, `"EOF"`, …)
- `TokenRegistry` no se usa

**Trampa:** `docs/LANGUAGE_CONFIG.md` dice que el **primero** del `order` gana. El código hace lo contrario. `language.config.json` está escrito “keywords primero”. Los tests del lexer y de application **invierten** el `order` para que keywords ganen a identifiers. Si cargás el JSON tal cual, `let` se tokeniza como `ID`.

Ver [modules/LEXER.md](modules/LEXER.md).

### `parser` — tokens a `SyntaxProgram`

Parser dirigido por la `Grammar`. No hay AST tipado de salida.

- `DefaultParser.parseNextStatement(lexer, program)` evalúa `grammar.start` (hoy `"statement"`)
- Handlers: `Atom`, `Seq`, `Or`, `Left`, `Repeat` — registrados en `RuleHandlers.defaults()`
- Cursor con backtracking: `TokenSource` / `LexerTokenSource` (`checkpoint` / `restore`)
- `Or`: primera alternativa que matchea; si falla, restaura
- `Seq`: si falla el **primer** step → `null` + restore; si falla **después** → `ParseException`
- `Left`: expresiones infix asociativas a izquierda; envuelve el operando aunque no haya operador
- `Repeat`: cero o más; aborta si una iteración no consume tokens
- Errores: `ParseException` (sintaxis). No hace semántica

Ver [modules/PARSER.md](modules/PARSER.md).

### `type-checker` — tipos y símbolos (a futuro)

Va **después del parser**. Debe caminar `SyntaxProgram` / `SyntaxNode`.

El código actual está en Gradle `:semantic` y analiza el AST tipado (`Program`). Hay que migrarlo, no cablearlo así.

Chequea (reglas a conservar):

- initializer compatible con la anotación (`number` / `string`)
- redeclaración
- identificador no declarado
- operandos de `+ - * /` (`+` acepta number+number o string+string)
- argumentos de calls (hoy solo recorre; el call no tiene tipo)

Ver [modules/TYPE_CHECKER.md](modules/TYPE_CHECKER.md).

### `interpreter` — ejecutar (a futuro)

Evalúa el programa validado (`println`, expresiones, eventualmente control de flujo). No existe módulo Gradle todavía.

Ver [modules/INTERPRETER.md](modules/INTERPRETER.md).

### `linter` — estilo / SCA (a futuro)

Reglas de estilo sobre el árbol. No existe módulo Gradle todavía. No es type-check.

Ver [modules/LINTER.md](modules/LINTER.md).

### `formatter` — pretty-print (a futuro)

Reescribe el programa con formato canónico. No existe módulo Gradle todavía. No es linter: el linter reporta, el formatter emite código.

Ver [modules/FORMATTER.md](modules/FORMATTER.md).

### `infrastructure` — I/O y serializers

Único módulo con kotlinx.serialization.

- `JSONLanguageConfigReader` / `JSONGrammarConfigReader` implementan los ports de `common`
- Serializers **surrogate** en `serializer/config`: el dominio no lleva `@Serializable`
- Discriminación de `GrammarRule` por **clave JSON** (`or`, `seq`, `left`, `atom`, `repeat`), no por campo `type`
- `TokenRule` sí usa `type: "exact" | "regex"`
- `FileCodeReader`: `CodeReader` sobre un path de filesystem
- Resources: `language.config.json`, `grammar.config.json`

Ver [modules/INFRASTRUCTURE.md](modules/INFRASTRUCTURE.md).

### `application` — orquestación

- `interpretCode(langConfig, grammar, path): SyntaxProgram` — lex + parse streaming
- `Main.kt` vacío
- Tests de integración con archivos `.ps` y un DSL `assertAst { node(...) }`

Ver [modules/APPLICATION.md](modules/APPLICATION.md).

### `build-logic` — calidad del repo (no del lenguaje)

Included build. Convention plugin `printscript.quality`: ktlint (`ktlintCheck` / `ktlintFormat`) + detekt. Se aplica a los subproyectos desde el `build.gradle.kts` raíz. **No** es el linter/formatter de PrintScript.

Ver [modules/BUILD_LOGIC.md](modules/BUILD_LOGIC.md).

---

## Principios de diseño (no romperlos)

1. **Dominio sin serialización.** Clases de `common` no tienen `@Serializable`. JSON vive en `infrastructure` con `KSerializer` + surrogate.
2. **Tokens genéricos.** `Token.type: String`. El enum `TokenType` es leftover; no lo uses en código nuevo.
3. **Parser genérico.** El parser no conoce `let` ni `println`. Esas palabras están en los JSON. Un handler nuevo = data class en `common` + serializer + `RuleHandler` + registro.
4. **Árbol de sintaxis ≠ AST tipado.** El pipeline nuevo camina `SyntaxNode` (`child` / `find` / `value`). El AST de `ASTDefinition.kt` es leftover de `:semantic`; no es el destino del type-checker.
5. **Errores por capa.** Lexer tira `Error` / `IllegalStateException`. Parser tira `ParseException`. Type-checker acumula errores y devuelve éxito/fallo (no lanza). Interpreter (cuando exista) reporta errores de runtime.
6. **Streaming.** Ni lexer ni parser cargan el programa entero de una: caracteres → tokens on demand → un statement por llamada.

---

## Lenguaje v1 — tokens y gramática (lo que hay que mantener alineado)

Los strings de token del lexer **tienen que coincidir** con los de la gramática.

| Token string | Origen típico | Capture |
|---|---|---|
| `LET` | `let` | no |
| `CALL` | `println` | sí (`"println"`) |
| `TYPE` | `string`, `number` | sí |
| `ID` | identificadores | sí |
| `NUMBER_LITERAL` | `42`, `1.5` | sí |
| `STRING_LITERAL` | `"hola"` (con comillas) | sí |
| `OPERATOR` | `+ - * /` | sí (el símbolo) |
| `COLON` `ASSIGN` `SEMICOLON` `LEFT_PAREN` `RIGHT_PAREN` `COMMA` | puntuación | no |
| `EOF` | fin de archivo (lo emite el lexer, no una regla) | no |

Gramática (`start = statement`):

- `statement` = `variable` \| `expression-stmt`
- `variable` = `LET` ID `:` TYPE `=` expression `;`
- `expression-stmt` = expression `;`
- `expression` = `term` (`+`\|`-` `term`)*
- `term` = `factor` (`*`\|`/` `factor`)*
- `factor` = number \| string \| identifier \| call \| group
- `call` = CALL `(` expression `)` — un solo argumento

---

## Forma del árbol (`SyntaxNode`)

Nombres de nodo = nombres de regla (o el `Token.type` si es un token capturado).

Ejemplo `let x: number = 1 + 2 * 3;`:

```
variable
  ID("x")
  TYPE("number")
  expression
    term → number("1")
    OPERATOR("+")
    term
      number("2")
      OPERATOR("*")
      number("3")
```

`LeftRule` **siempre envuelve**: un literal suelto igual queda `expression → term → number`. Por eso los tests de application asertan esa profundidad. No “aplanes” el árbol.

Helpers: `child(name)`, `find(name)` (DFS), `value()` (exige token con value).

---

## Huecos y leftover (estado real del repo)

Tratalos como deuda conocida, no como “código muerto a borrar en silencio” salvo que te lo pidan.

| Qué | Dónde | Impacto |
|---|---|---|
| Type-checker no está cableado | `InterpretCode.kt` (bloque comentado) | El pipeline termina en el parse tree |
| `:semantic` espera AST tipado | `DefaultSemanticAnalyzer.analyze(Program)` | Migrar a type-checker sobre `SyntaxNode`; no hacer lowering al AST viejo |
| Interpreter no existe | — | No hay ejecución de `println` |
| Linter no existe | — | No hay reglas de estilo |
| Formatter no existe | — | No hay pretty-print |
| `order` del lexer invertido vs docs/JSON | `RuleDrawResolver` vs `language.config.json` | Cargar el JSON sin invertir keywords pierde contra identifiers |
| `TokenType` enum | `common/.../TokenType.kt` | No lo usa nadie |
| `TokenRegistry` | `lexer/.../TokenRegistry.kt` | No lo usa el `TokenStream` |
| `Main.kt` vacío | `application` | No hay CLI |
| `repeat` listo, no usado en v1 | grammar + `RepeatRuleHandler` | Sirve para `if` / bloques |
| `COMMA` tokenizado, no parseado | language config | Pensado para args múltiples |
| kotlinx-collections-immutable | lexer y semantic | Usado en tests del lexer (`TokenLister`); semantic lo declara y casi no lo usa |

---

## Cómo extender (recetas cortas)

### Nuevo token (keyword, operador, literal)

1. Agregar regla en `language.config.json` (y en los `LanguageConfig` de test: `PrintScriptLanguage`, `MockLexerFactory`).
2. Poner la categoría en `order` **al final si tiene que ganar** (el resolver usa índice máximo). Keywords tienen que estar **después** de identifiers.
3. Si el parser lo consume: usarlo en `grammar.config.json` (`"LET"` o `{ "capture": "ID" }`).
4. Ver [LANGUAGE_CONFIG.md](LANGUAGE_CONFIG.md).

### Nueva producción de gramática

1. Editar `grammar.config.json`.
2. Si alcanza con `or`/`seq`/`left`/`atom`/`repeat`, no hay código Kotlin nuevo.
3. Tests: `parser` (handlers + `ParserTest`) y un `.ps` en application si es de integración.
4. Ver [GRAMMAR_CONFIG.md](GRAMMAR_CONFIG.md).

### Nuevo **tipo** de regla de gramática (no solo una producción)

1. Data class en `common` implementando `GrammarRule` (`references()`).
2. Serializer surrogate en `infrastructure` + rama en `GrammarRuleSerializer.select`.
3. `RuleHandler` en `parser` + entrada en `RuleHandlers.defaults()`.
4. Tests de handler y de deserialización.

### Type-checker

Migrar `:semantic` a un módulo `type-checker` que recorra `SyntaxNode`. Cablearlo en `application` **después** del parser y **antes** del interpreter. Detalle en [modules/TYPE_CHECKER.md](modules/TYPE_CHECKER.md).

No construyas un lowering `SyntaxProgram` → `Program` para reusar el AST tipado.

### Interpreter / linter / formatter

Módulos nuevos. Docs vacíos: [modules/INTERPRETER.md](modules/INTERPRETER.md), [modules/LINTER.md](modules/LINTER.md), [modules/FORMATTER.md](modules/FORMATTER.md).

---

## Tests — qué corre qué

| Módulo | Qué cubren |
|---|---|
| `lexer` | Tokenización de `let`/`println` y strings no cerrados; prioridad del resolver |
| `parser` | Cada handler, gramática PrintScript completa (precedencia, parens, errores), `Grammar` validation, `SyntaxNode` |
| `semantic` (a migrar a type-checker) | AST tipado fabricado a mano (no pasa por lexer/parser) |
| `infrastructure` | `JSONGrammarConfigReader` contra el resource real + JSON de `repeat` |
| `application` | 3 archivos `.ps` end-to-end lex+parse, asertando forma del `SyntaxProgram` |

Correr: `./gradlew test` (o `:lexer:test`, etc.).

---

## Invariantes que un agente no debe violar

- No pongas `@Serializable` en `common`.
- No hagas que el parser conozca nombres de keyword; eso va en JSON.
- No asumas que `TokenType.IDENTIFIER` existe en runtime: el lexer emite `"ID"`.
- No asumas que `LanguageConfig.order[0]` es la categoría más prioritaria — es la **menos**.
- `Grammar(...)` explota si `start` o una referencia no existen; no construyas gramáticas a mano sin pasar por eso.
- `DefaultParser` cachea el `TokenSource` por identidad del `Lexer`: no reutilices un parser con **otro** lexer sin un parser nuevo (o el bind se queda corto si es el mismo objeto).
- Locations: `FileCodeReader` arranca en `(1,1)`; el `MockReader` de tests del lexer arranca línea `0`. No compares locations entre esos dos mundos.

---

## Por dónde empezar según la tarea

| Tarea | Leer primero | Tocar |
|---|---|---|
| Cambiar qué tokens existen | LANGUAGE_CONFIG + lexer + infrastructure JSON | `language.config.json`, tests de lexer/application |
| Cambiar sintaxis | GRAMMAR_CONFIG + parser | `grammar.config.json`, `ParserTest`, ejemplos `.ps` |
| Nuevo combinador de gramática | parser + infrastructure serializers + common domain | 3 módulos a la vez |
| Tipos / variables no declaradas | TYPE_CHECKER + SyntaxNode | migrar `:semantic`; no usar `ASTDefinition.kt` |
| Ejecutar el programa | INTERPRETER | módulo a futuro |
| Reglas de estilo | LINTER | módulo a futuro |
| Pretty-print | FORMATTER | módulo a futuro |
| Lint/format del Kotlin del repo | BUILD_LOGIC | `build-logic` / `printscript.quality` |
| CLI / correr un archivo | application | `Main.kt`, `interpretCode` |
| Leer un `.ps` de otro lado (stdin, string) | common `CodeReader` + infrastructure | nueva impl de `CodeReader` |
