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
| `common` | [modules/COMMON.md](modules/COMMON.md) | Tipos compartidos: Token, Grammar, SyntaxNode, TypeSystemConfig, Result, errores, SideEffect |
| `lexer` | [modules/LEXER.md](modules/LEXER.md) | Código → tokens, matching exact/regex, prioridad por categoría |
| `parser` | [modules/PARSER.md](modules/PARSER.md) | Tokens → `SyntaxProgram` evaluando `Grammar` |
| `type-checker` | [modules/TYPE_CHECKER.md](modules/TYPE_CHECKER.md) | Tipos y símbolos sobre el parse tree. Cableado en `interpretCode` |
| `interpreter` | [modules/INTERPRETER.md](modules/INTERPRETER.md) | `SyntaxProgram` → `List<SideEffect>`. Módulo listo, **no cableado** en application |
| `infrastructure` | [modules/INFRASTRUCTURE.md](modules/INFRASTRUCTURE.md) | JSON + filesystem: configs y `FileCodeReader` |
| `application` | [modules/APPLICATION.md](modules/APPLICATION.md) | Caso de uso `interpretCode`: lex + parse + type-check |
| `formatter` | [modules/FORMATTER.md](modules/FORMATTER.md) | Pretty-print / check de whitespace. Cableado en `FormatCode` / `CheckFormat` |

### Módulos a futuro (pipeline)

| Módulo | Archivo | Una línea |
|---|---|---|
| `linter` | [modules/LINTER.md](modules/LINTER.md) | Reglas de estilo / análisis estático. Vacío — a futuro |

### Build (no es pipeline)

| Módulo | Archivo | Una línea |
|---|---|---|
| `build-logic` | [modules/BUILD_LOGIC.md](modules/BUILD_LOGIC.md) | Included build: convention plugin `printscript.quality` (ktlint + detekt) |

### Configuración del lenguaje

| Archivo | Qué cubre |
|---|---|
| [LANGUAGE_CONFIG.md](configs/LANGUAGE_CONFIG.md) | Forma de `language.config.json` (reglas exact/regex del lexer) |
| [GRAMMAR_CONFIG.md](configs/GRAMMAR_CONFIG.md) | Forma de `grammar.config.json` (producciones del parser) |
| [TYPE_SYSTEM_CONFIG.md](configs/TYPE_SYSTEM_CONFIG.md) | Forma de `type-system.config.json` (tipos, ops, nodos) |

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

Hoy el pipeline de `interpretCode` **corta en el type-checker**: lexea, parsea y valida tipos. Si hay errores de tipo, `interpretCode` falla. El formatter está cableado en `FormatCode` / `CheckFormat` (lex + parse, **sin** type-check: lo decide application, no el módulo). El interpreter existe como módulo Gradle con tests y **no** está en las dependencias de application. Linter no existe.

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
DefaultTypeChecker      type-checker       validar tipos / símbolos
    │                                      type-system.config.json
    ▼
DefaultInterpreter      interpreter        SyntaxProgram → List<SideEffect>
                                           módulo listo; no lo llama interpretCode

DefaultFormatter        formatter          SyntaxProgram → String / Report
                                           FormatCode / CheckFormat; no lo llama interpretCode
```

Linter no está en esa cadena. El formatter corre sobre el árbol post-parser; no pide ni asume type-check. Ver [modules/LINTER.md](modules/LINTER.md) y [modules/FORMATTER.md](modules/FORMATTER.md).

Armado típico (lo que hace `interpretCode` **hoy**):

```kotlin
val codeReader = FileCodeReader(path)
val lexer = DefaultLexerFactory.create(codeReader, langConfig)
val parser = DefaultParserFactory.create(grammar)

var program = SyntaxProgram.empty()
while (lexer.peek(null).type != "EOF") {
    program = parser.parseNextStatement(lexer, program)
}

val report = DefaultTypeCheckerFactory.create(typeSystem).check(program)
if (!report.isOk) error("El chequeo de tipos falló:…")
return program
```

El parser es **streaming por statement**: no parsea el archivo de una. Cada llamada consume tokens hasta terminar un `statement` (regla `start` de la gramática).

Para **ejecutar**, el caller tendría que hacer (hoy solo lo hacen los tests de `:interpreter`):

```kotlin
DefaultInterpreterFactory.create().interpret(InterpreterContext(), program)
// Result<List<SideEffect>, RuntimeError>
```

---

## Mapa de módulos Gradle

```
settings.gradle.kts incluye:
  common, lexer, infrastructure, parser, type-checker, application, interpreter, formatter
  pluginManagement { includeBuild("build-logic") }  — convention plugin, no es library
```

No hay módulo `:semantic`.

Dependencias de **producción**:

```
common          ← nadie (solo JDK)
lexer           ← common
parser          ← common, lexer
type-checker    ← common
interpreter     ← common
formatter       ← common
infrastructure  ← common   (+ kotlinx.serialization-json + kaml)
application     ← common, lexer, parser, type-checker, formatter, infrastructure
                  (no depende de interpreter)
```

Dependencias extra de **test**:

- `parser` testImplementation `infrastructure` (carga `grammar.config.json`)
- `formatter` testImplementation `infrastructure` (carga `formatter-language.json`)
- `interpreter` testImplementation `lexer`, `parser`, `infrastructure` (lex+parse+interpret)
- `application` tests usan `JSONGrammarConfigReader` + `JSONTypeSystemConfigReader` + un `LanguageConfig` armado en código (`PrintScriptLanguage`), no el JSON del lexer tal cual

Toolchain: `kotlin.jvmToolchain(21)`. Version catalog: `gradle/libs.versions.toml`.

Paquetes:

- Todo lo reutilizable: `printscript.*`
- Application producción: `usecases` (`InterpretCode`)
- Application tests: `edu.austral.dissis`

---

## Resumen de cada módulo

### `common` — contrato compartido

Única fuente de tipos. **No** tiene serializers ni I/O concreto.

- **Léxico:** `Token` (`type` es `String`, no el enum), `TokenRule` (`ExactRule` / `RegexRule`), `LanguageConfig`
- **Gramática:** `Grammar` (valida start + referencias al construirse), `GrammarRule` (`Or`, `Atom`, `Seq`, `Left`, `Repeat`), `SeqStep` (`TokenStep`, `RuleRefStep`), `OperatorSpec`
- **Árbol que produce el parser:** `SyntaxNode` / `SyntaxProgram` (nombres = reglas de la gramática)
- **Type-system:** `TypeSystemConfig` / `Operation` / `NodeConfig` (valida tipos referenciados al construirse)
- **Resultados:** `Result` / `Report` en `util/` (`map` / `flatMap` / `fold`)
- **Errores:** `TypeError` sealed (`TypeMismatch`, `Redeclaration`, `UndeclaredIdentifier`, …) y `RuntimeError` sealed (`DivisionByZero`, `InvalidLiteral`, `UnresolvableCall`, …). Algunas variantes implementan **ambos** (`UndeclaredIdentifier`, `InvalidOperands`, `UnrecognizedNode`). El módulo `:type-checker` **no** usa este sealed: tiene su propio `printscript.typechecker.TypeError` (data class con `message` + `location`)
- **Efectos:** `SideEffect` / `PrintEffect` (lo que emite el interpreter)
- **Puertos:** `CodeReader`, `LanguageConfigReader`, `GrammarConfigReader`, `TypeSystemConfigReader`
- **Ubicación:** `Location` + `CharPosition`

Ver [modules/COMMON.md](modules/COMMON.md).

### `lexer` — código a tokens

`TokenStream` lee caracteres, salta whitespace, y agranda el lexema mientras alguna regla sea `VALID` o `PARTIAL`. Cuando el próximo carácter deja todo `INVALID`, emite el token del match previo.

- Matching: `RuleEvaluator` (exact = igualdad/prefijo; regex = `matcher` + `partial`)
- Empate entre categorías: `RuleDrawResolver` — **gana la categoría con mayor índice en `LanguageConfig.order`** (la **última** de la lista)
- Factory: `DefaultLexerFactory.create(codeReader, langConfig)`
- `Token.type` es el string `token` de la regla (`"LET"`, `"ID"`, `"NUMBER_LITERAL"`, `"EOF"`, …)
- `TokenRegistry` no se usa

**Trampa:** el código gana con la **última** categoría de `order` ([LANGUAGE_CONFIG.md](configs/LANGUAGE_CONFIG.md)). `language.config.json` está escrito “keywords primero”. Los tests del lexer, de application y de interpreter **invierten** el `order` para que keywords ganen a identifiers. Si cargás el JSON tal cual, `let` se tokeniza como `ID`.

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

### `type-checker` — tipos y símbolos

Va **después del parser**. Camina `SyntaxProgram` / `SyntaxNode`. Factory: `DefaultTypeCheckerFactory.create(config)`. **Está cableado** en `interpretCode`.

Chequea:

- initializer compatible con la anotación (`number` / `string`)
- redeclaración
- identificador no declarado
- operandos de `+ - * /` según `type-system.config.json` (`+` number+number, string+string, string+number; permutación si `commutative`)
- argumentos de calls (el call no tiene tipo de retorno)

`check` acumula; `checkStrict` corta en el primero. No lanza: `application` traduce el `Report`.

Ver [modules/TYPE_CHECKER.md](modules/TYPE_CHECKER.md).

### `interpreter` — ejecutar

Módulo Gradle `:interpreter` (`implementation` solo `common`). Recorre el `SyntaxProgram` y devuelve `Result<List<SideEffect>, RuntimeError>` (fail-fast, Result end-to-end). **No** está en el classpath de `application`: `interpretCode` no lo llama.

Dos niveles de dispatch:

1. `NodeKindResolver` traduce `node.name` (regla de la gramática) a `NodeKind` vía `PrintScriptMapping`.
2. `DefaultInterpreter` despacha statements (`VariableDeclarationExecutor`, `ExpressionStatementExecutor`); `ExpressionSolver` despacha expresiones.

`println` es una **expresión** (`factor → call`), no un statement. Los efectos viajan en `EvalResult(value, sideEffects)` y se combinan de hijos a padres. `CallEvaluator` hardcodea el callee `"println"` y emite `PrintEffect`.

Valores: `NumberValue(Double)`, `StringValue` (sin comillas), `UnitValue` (resultado de un call). Números enteros se imprimen sin `.0` (`toPrintableString()`).

Contexto: `InterpreterContext` inmutable copy-on-write, con `parent` y `childScope()`. `declareVariable` sombrea en el scope actual; `assignVariable` reconstruye la cadena dueña. V1 no tiene asignaciones sueltas ni bloques, así que `assignVariable` / `childScope` están listos y sin usar en el walk de statements.

Tipos en runtime: `DefaultTypeConfiguration` es una tabla **hardcodeada** (no lee `type-system.config.json`). Tiene `number` con `+ - * /` y `string + string`. **No** tiene `string + number` (el type-checker sí). División por cero: guard explícito → `DivisionByZero`. El interpreter **no** chequea la anotación `TYPE` de un `let`: confía en el programa validado.

Ver [modules/INTERPRETER.md](modules/INTERPRETER.md).

### `linter` — estilo / SCA (a futuro)

Reglas de estilo sobre el árbol. No existe módulo Gradle todavía. No es type-check.

Ver [modules/LINTER.md](modules/LINTER.md).

### `formatter` — pretty-print

Módulo Gradle `:formatter`. Recibe `SyntaxProgram` y produce texto canónico (`format` → `Result`) o un `Report` de mismatches (`check`). Strategy: `addChar(point, char)` + registry (newlines + máx. un espacio; el cap no es una rule). Rules de lenguaje (operadores, `;`+newline, `let`+espacio) y de usuario (`:` / `=` / newlines antes de `println`, con defaults). Reconstruye `let` / `:` / `=` / `;` / parens. Application: `FormatCode` / `CheckFormat`.

Ver [modules/FORMATTER.md](modules/FORMATTER.md).

### `infrastructure` — I/O y serializers

Único módulo con kotlinx.serialization.

- `JSONLanguageConfigReader` / `JSONGrammarConfigReader` / `JSONTypeSystemConfigReader` / `JSONFormatterRulesConfigReader` / `YAMLFormatterRulesConfigReader` implementan los ports de `common`
- Serializers **surrogate** en `serializer/config`: el dominio no lleva `@Serializable`
- Discriminación de `GrammarRule` por **clave JSON** (`or`, `seq`, `left`, `atom`, `repeat`), no por campo `type`
- `TokenRule` sí usa `type: "exact" | "regex"`
- Formatter: el mismo `FormatterRulesConfigSerializer` sirve para JSON (kotlinx) y YAML (kaml)
- `FileCodeReader`: `CodeReader` sobre un path de filesystem
- Resources: `language.config.json`, `grammar.config.json`, `type-system.config.json`, `formatter-language.json`

Ver [modules/INFRASTRUCTURE.md](modules/INFRASTRUCTURE.md).

### `application` — orquestación

- `InterpretCode.interpretCode(...)` — lex + parse + type-check
- `FormatCode.formatCode(...)` / `CheckFormat.checkFormat(...)` — lex + parse + format; check compara source vs `format()`
- No hay `Main.kt` ni CLI
- Tests de integración con archivos `.ps` y un DSL `assertAst { node(...) }`

Ver [modules/APPLICATION.md](modules/APPLICATION.md).

### `build-logic` — calidad del repo (no del lenguaje)

Included build. Convention plugin `printscript.quality`: ktlint (`ktlintCheck` / `ktlintFormat`) + detekt, y `installGitHooks` en el root. Se aplica al root y a los subproyectos desde el `build.gradle.kts` raíz. **No** es el linter/formatter de PrintScript.

Ver [modules/BUILD_LOGIC.md](modules/BUILD_LOGIC.md).

---

## Principios de diseño (no romperlos)

1. **Dominio sin serialización.** Clases de `common` no tienen `@Serializable`. JSON vive en `infrastructure` con `KSerializer` + surrogate.
2. **Tokens genéricos.** `Token.type: String`. El enum `TokenType` es leftover; no lo uses en código nuevo.
3. **Parser genérico.** El parser no conoce `let` ni `println`. Esas palabras están en los JSON. Un handler nuevo = data class en `common` + serializer + `RuleHandler` + registro.
4. **Árbol de sintaxis.** El pipeline camina `SyntaxNode` (`child` / `find` / `value`). No hay AST tipado aparte.
5. **Errores por capa.** Lexer tira `Error` / `IllegalStateException`. Parser tira `ParseException`. Type-checker acumula en `Report` / `Result` (su `TypeError` data class, no lanza). `interpretCode` sí lanza si el report no es ok. Interpreter reporta `Result.Err(RuntimeError)` y no lanza.
6. **Streaming.** Ni lexer ni parser cargan el programa entero de una: caracteres → tokens on demand → un statement por llamada.

El interpreter **sí** conoce `"println"` (en `CallEvaluator`) y los nombres de regla v1 (en `PrintScriptMapping`). Extenderlo es registrar executor/evaluator, no tocar el motor de dispatch.

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

El interpreter se apoya en esa forma:

- En un `variable`, el id es hijo `"ID"` (captura de seq), no `"identifier"`
- El operador de un binario está en `children[1]` (hoja `OPERATOR`)
- Un `expression`/`term` sin operador tiene **un solo hijo**; `BinaryOperationEvaluator` lo trata como passthrough

---

## Huecos y leftover (estado real del repo)

Tratalos como deuda conocida, no como “código muerto a borrar en silencio” salvo que te lo pidan.

| Qué | Dónde | Impacto |
|---|---|---|
| Interpreter no cableado en application | `application/build.gradle.kts`, `InterpretCode.kt` | `interpretCode` no ejecuta `println`; no hay `List<SideEffect>` desde el caso de uso |
| Linter no existe | — | No hay reglas de estilo |
| No hay CLI | no existe `Main.kt` | No hay entrada `args[0]` |
| `string + number` diverge | type-system JSON vs `DefaultTypeConfiguration` | El type-checker acepta `"a" + 1`; el interpreter responde `InvalidOperands` |
| Tabla de ops del interpreter hardcodeada | `interpreter/.../DefaultTypeConfiguration.kt` | No comparte `type-system.config.json` con el type-checker |
| `order` del lexer invertido vs docs/JSON | `RuleDrawResolver` vs `language.config.json` | Cargar el JSON sin invertir keywords pierde contra identifiers |
| `partial` de números en el JSON | `language.config.json` (`^[0-9]`) | `1.5` se parte en el lexer; los tests del interpreter usan un partial más amplio |
| Dos `TypeError` | `common/.../error/TypeError.kt` vs `type-checker/.../TypeError.kt` | El pipeline de application usa el data class del módulo; el sealed de common lo usa el interpreter (variantes compartidas) |
| `TokenType` enum | `common/.../TokenType.kt` | No lo usa nadie |
| `TokenRegistry` | `lexer/.../TokenRegistry.kt` | No lo usa el `TokenStream` |
| `repeat` listo, no usado en v1 | grammar + `RepeatRuleHandler` | Sirve para `if` / bloques |
| `COMMA` tokenizado, no parseado | language config | Pensado para args múltiples |
| kotlinx-collections-immutable | lexer | Usado en tests del lexer (`TokenLister`) |

---

## Cómo extender (recetas cortas)

### Nuevo token (keyword, operador, literal)

1. Agregar regla en `language.config.json` (y en los `LanguageConfig` de test: `PrintScriptLanguage`, `MockLexerFactory`, `PsSupport` del interpreter).
2. Poner la categoría en `order` **al final si tiene que ganar** (el resolver usa índice máximo). Keywords tienen que estar **después** de identifiers.
3. Si el parser lo consume: usarlo en `grammar.config.json` (`"LET"` o `{ "capture": "ID" }`).
4. Ver [LANGUAGE_CONFIG.md](configs/LANGUAGE_CONFIG.md).

### Nueva producción de gramática

1. Editar `grammar.config.json`.
2. Si alcanza con `or`/`seq`/`left`/`atom`/`repeat`, no hay código Kotlin nuevo.
3. Tests: `parser` (handlers + `ParserTest`) y un `.ps` en application si es de integración.
4. Ver [GRAMMAR_CONFIG.md](configs/GRAMMAR_CONFIG.md).

### Nuevo **tipo** de regla de gramática (no solo una producción)

1. Data class en `common` implementando `GrammarRule` (`references()`).
2. Serializer surrogate en `infrastructure` + rama en `GrammarRuleSerializer.select`.
3. `RuleHandler` en `parser` + entrada en `RuleHandlers.defaults()`.
4. Tests de handler y de deserialización.

### Nuevo tipo u operador (sin kind nuevo)

1. Editar `type-system.config.json` (`types`, `literals`, `operations`) — eso alimenta al **type-checker**.
2. Si el lenguaje lo escribe: token en `language.config.json` + producción en grammar.
3. Si el interpreter tiene que ejecutarlo: regla en `DefaultTypeConfiguration` (hoy no lee el JSON).
4. Ver [TYPE_SYSTEM_CONFIG.md](configs/TYPE_SYSTEM_CONFIG.md).

### Type-checker (kind nuevo)

Ya camina `SyntaxNode` y está cableado después del parser. Nuevo *kind* → handler en `:type-checker`. Detalle en [modules/TYPE_CHECKER.md](modules/TYPE_CHECKER.md).

### Interpreter (construcción nueva)

Módulo existente. Nuevo *kind* → entrada en `NodeKind` + `PrintScriptMapping` + executor o evaluator en `DefaultInterpreterFactory`. Detalle en [modules/INTERPRETER.md](modules/INTERPRETER.md). Cablearlo: dependencia en `application` + llamada después del type-check en `InterpretCode.kt`.

### Formatter (rule nueva)

1. `FormatRule` + `FormatRuleFactory` en `:formatter`.
2. Registrar en `FormatRuleFactories.defaults()`.
3. Lenguaje → `formatter-language.json`. Usuario → YAML (`FormatterConfig.USER_YAML_PATH`); si hay campo nuevo (`enabled`, `count`, …) extender el surrogate del serializer.
4. Tests de `format` y `check`. Detalle: [modules/FORMATTER.md](modules/FORMATTER.md).

### Linter

Módulo a futuro. Docs vacíos: [modules/LINTER.md](modules/LINTER.md).

---

## Tests — qué corre qué

| Módulo | Qué cubren |
|---|---|
| `lexer` | Tokenización de `let`/`println` y strings no cerrados; prioridad del resolver |
| `parser` | Cada handler, gramática PrintScript completa (precedencia, parens, errores), `Grammar` validation, `SyntaxNode` |
| `type-checker` | Scope, resolver (literales, binarios, permutación), `TypeChecker` (match/mismatch/redeclare), `check` vs `checkStrict` |
| `interpreter` | contexto (scope/shadow/assign), evaluators (literales/binarios/calls/div-cero), executors, integración lex+parse+interpret con `SideEffect` |
| `formatter` | `format`/`check` de `1+2`, registry, loader (type desconocido / type fijo en user), JSON real |
| `infrastructure` | `JSONGrammarConfigReader` y `JSONTypeSystemConfigReader` contra el resource real + JSON de `repeat`; readers JSON/YAML del formatter |
| `common` | `Result`/`Report`, `TypeSystemConfig` (tipos referenciados), variantes de `TypeError` |
| `application` | `.ps` end-to-end lex+parse+type-check; format/check de `1+2;` vs `1 + 2;` |

Correr: `./gradlew test` (o `:lexer:test`, etc.). CI: `.github/workflows/tests.yml` corre `test` de todos los módulos; `lint.yml` corre `detekt`; `format.yml` corre `ktlintCheck`.

---

## Invariantes que un agente no debe violar

- No pongas `@Serializable` en `common`.
- No hagas que el parser conozca nombres de keyword; eso va en JSON.
- No asumas que `TokenType.IDENTIFIER` existe en runtime: el lexer emite `"ID"`.
- No asumas que `LanguageConfig.order[0]` es la categoría más prioritaria — es la **menos**.
- `Grammar(...)` explota si `start` o una referencia no existen; no construyas gramáticas a mano sin pasar por eso.
- `DefaultParser` cachea el `TokenSource` por identidad del `Lexer`: no reutilices un parser con **otro** lexer sin un parser nuevo (o el bind se queda corto si es el mismo objeto).
- Locations: `FileCodeReader` arranca en `(1,1)`; el `MockReader` de tests del lexer arranca línea `0`. No compares locations entre esos dos mundos.
- No aplanes `expression`/`term` de un solo hijo: el interpreter y los tests de application dependen del wrap de `LeftRule`.
- `DefaultInterpreter` exige en construcción que todo `NodeKind` del mapping tenga executor **o** evaluator; duplicados de kind explotan igual.

---

## Por dónde empezar según la tarea

| Tarea | Leer primero | Tocar |
|---|---|---|
| Cambiar qué tokens existen | LANGUAGE_CONFIG + lexer + infrastructure JSON | `language.config.json`, tests de lexer/application/interpreter |
| Cambiar sintaxis | GRAMMAR_CONFIG + parser | `grammar.config.json`, `ParserTest`, ejemplos `.ps` |
| Nuevo combinador de gramática | parser + infrastructure serializers + common domain | 3 módulos a la vez |
| Tipos / variables no declaradas | TYPE_CHECKER + TYPE_SYSTEM_CONFIG | `:type-checker` + `type-system.config.json` |
| Ejecutar el programa | INTERPRETER | ya existe `:interpreter`; cablear en `application/InterpretCode.kt` |
| Nueva construcción a ejecutar | INTERPRETER | `NodeKind` + executor/evaluator + mapping |
| Reglas de estilo | LINTER | módulo a futuro |
| Pretty-print | FORMATTER | indent de bloques cuando existan `if` / `{` |
| Lint/format del Kotlin del repo | BUILD_LOGIC | `build-logic` / `printscript.quality` |
| CLI / correr un archivo | application | crear `Main.kt`, llamar `interpretCode` (y el interpreter si querés output) |
| Leer un `.ps` de otro lado (stdin, string) | common `CodeReader` + infrastructure | nueva impl de `CodeReader` |
