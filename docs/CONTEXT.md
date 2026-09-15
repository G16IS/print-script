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
| `interpreter` | [modules/INTERPRETER.md](modules/INTERPRETER.md) | `SyntaxProgram` → `List<SideEffect>`. Cableado en `ExecuteCode` (CLI `run`) |
| `infrastructure` | [modules/INFRASTRUCTURE.md](modules/INFRASTRUCTURE.md) | JSON/YAML + `FileCodeReader`. No es el CLI |
| `application` | [modules/APPLICATION.md](modules/APPLICATION.md) | Casos de uso: type-check, execute, format, check, lint |
| `formatter` | [modules/FORMATTER.md](modules/FORMATTER.md) | Pretty-print / check de whitespace. Cableado en `FormatCode` / `CheckFormat` |
| `linter` | [modules/LINTER.md](modules/LINTER.md) | Reglas de estilo sobre el árbol. Cableado en `LintProgram` |
| `cli` | [modules/CLI.md](modules/CLI.md) | Composition root: carga configs y llama use cases (`run`/`lint`/`check`/`format`/`typecheck`) |

### Módulos a futuro (pipeline)

Ninguno: el pipeline de v1 está cableado. El interpreter se llama desde `ExecuteCode` (`run`); no desde `interpretCode`.

### Build (no es pipeline)

| Módulo | Archivo | Una línea |
|---|---|---|
| `build-logic` | [modules/BUILD_LOGIC.md](modules/BUILD_LOGIC.md) | Included build: convention plugin `printscript.quality` (ktlint + detekt) |

### Configuración del lenguaje

| Archivo | Qué cubre |
|---|---|
| [LANGUAGE_CONFIG.md](configs/LANGUAGE_CONFIG.md) | Forma de `language.config.v1.0.json` (reglas exact/regex del lexer) |
| [GRAMMAR_CONFIG.md](configs/GRAMMAR_CONFIG.md) | Forma de `grammar.config.v1.0.json` (producciones del parser) |
| [TYPE_SYSTEM_CONFIG.md](configs/TYPE_SYSTEM_CONFIG.md) | Forma de `type-system.config.v1.0.json` (tipos, ops, nodos) |
| [FORMATTER_CONFIG.md](configs/FORMATTER_CONFIG.md) | JSON de lenguaje (rules + bindings) y YAML de usuario (`type` + value) |

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

- Declaración: `let <id>: <string|number> (= <expr>)?;` — el `=` es un `OptionalRule` (`initializer` / `var-init`)
- Statement de expresión: `<expr>;` (incluye `println(<expr>);`)
- Expresiones: literales number/string (`"..."` o `'...'`), identificadores, `+ - * /`, agrupación `( )`
- Precedencia: `* /` sobre `+ -`; asociatividad izquierda
- `+` de strings es concatenación **a nivel semántico** (el parser no distingue)
- `let x: T;` type-checkea y declara `x`. Leerla en runtime es `UninitializedVariable` (no hay defaults)

No está en la gramática v1 (pero el parser ya sabe evaluar `repeat`, pensado para bloques/`if`):

- `if`, braces, asignaciones sueltas, múltiples argumentos en `println`

Hoy `interpretCode` **corta en el type-checker**: lexea, parsea y valida tipos. Si hay errores de tipo, `interpretCode` falla. La ejecución vive en `ExecuteCode` (type-check + interpreter) y la dispara el CLI `run`. El formatter está cableado en `FormatCode` / `CheckFormat` (lex + parse, **sin** type-check). El linter está cableado en `LintProgram`.

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
                                           lo llama ExecuteCode (CLI `run`)

DefaultFormatter        formatter          SyntaxProgram → String / Report
                                           FormatCode / CheckFormat; no lo llama interpretCode

DefaultLinter           linter             SyntaxProgram → Report de LintError
                                           LintProgram; no lo llama interpretCode
```

El formatter y el linter corren sobre el árbol post-parser; no piden ni asumen type-check. Ver [modules/LINTER.md](modules/LINTER.md) y [modules/FORMATTER.md](modules/FORMATTER.md).

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

Para **ejecutar**, `ExecuteCode` (CLI `run`) hace type-check y después:

```kotlin
DefaultInterpreterFactory.create().interpret(InterpreterContext(), program)
// Result<List<SideEffect>, RuntimeError>
```

---

## Mapa de módulos Gradle

```
settings.gradle.kts incluye:
  common, lexer, infrastructure, parser, type-checker, application, interpreter, formatter, linter, cli
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
linter          ← common
cli             ← common + clikt + application + infrastructure
                  (composition root: carga configs, llama use cases, presenta)
infrastructure  ← common (+ kotlinx.serialization-json + kaml)
application     ← common, lexer, parser, type-checker, interpreter, formatter, linter
                  (NO infrastructure; I/O se inyecta)
```

Dependencias extra de **test**:

- `parser` testImplementation `infrastructure` (carga `grammar.config.v1.0.json`)
- `formatter` testImplementation `infrastructure` (carga `formatter-language.v1.0.json`)
- `interpreter` testImplementation `lexer`, `parser`, `infrastructure` (lex+parse+interpret)
- `application` tests usan `JSONGrammarConfigReader` + `JSONTypeSystemConfigReader` + `FileCodeReader` + un `LanguageConfig` armado en código (`PrintScriptLanguage`), no el JSON del lexer tal cual
- `cli` tests arman un `.ps` temporal y llaman `PrintScriptCli.create().test(...)` (pipeline real)
- `infrastructure` tests cubren readers JSON/YAML contra resources

Toolchain: `kotlin.jvmToolchain(21)`. Version catalog: `gradle/libs.versions.toml`.

Paquetes:

- Todo lo reutilizable: `printscript.*`
- Application producción: `usecases` (`InterpretCode`, `ExecuteCode`, `FormatCode`, `CheckFormat`, `LintProgram`)
- CLI: `printscript.cli`
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
- **Errores:** sealed raíz `Error`. `TypeError` (`TypeMismatch`, `Redeclaration`, …), `RuntimeError` (`DivisionByZero`, `InvalidLiteral`, …), `FormatError` (`MissingLexeme`, `UnrecognizedFormatNode`, `WhitespaceMismatch`, …). Algunas variantes de tipo/runtime implementan **ambos** (`UndeclaredIdentifier`, `InvalidOperands`, `UnrecognizedNode`). El formatter usa `UnrecognizedFormatNode` para no chocar con ese `UnrecognizedNode`. El módulo `:type-checker` **no** usa el sealed de common: tiene su propio `printscript.typechecker.TypeError` (data class con `message` + `location`)
- **Efectos:** `SideEffect` / `PrintEffect` (lo que emite el interpreter)
- **Puertos:** `CodeReader`, `LanguageConfigReader`, `GrammarConfigReader`, `TypeSystemConfigReader`
- **Ubicación:** `Location` + `CharPosition`

Ver [modules/COMMON.md](modules/COMMON.md).

### `lexer` — código a tokens

`TokenStream` lee caracteres, salta whitespace, y agranda el lexema mientras alguna regla sea `VALID` o `PARTIAL`. Cuando el próximo carácter deja todo `INVALID`, emite el token del match previo.

- Matching: `RuleEvaluator` (exact = igualdad/prefijo; regex = `matcher` + `partial`)
- Empate entre categorías: `RuleDrawResolver` — **gana la categoría con menor índice en `LanguageConfig.order`** (la **primera** de la lista)
- Factory: `DefaultLexerFactory.create(codeReader, langConfig)`
- `Token.type` es el string `token` de la regla (`"LET"`, `"ID"`, `"NUMBER_LITERAL"`, `"EOF"`, …)
- `TokenRegistry` no se usa

JSON y código coinciden: `order` es keywords → types → operators → literals → identifiers. Si invertís esa lista, `let` se tokeniza como `ID`.

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
- operandos de `+ - * /` según `type-system.config.v1.0.json` (`+` number+number, string+string, string+number; permutación si `commutative`)
- argumentos de calls (el call no tiene tipo de retorno)

`check` acumula; `checkStrict` corta en el primero. No lanza: `application` traduce el `Report`.

Ver [modules/TYPE_CHECKER.md](modules/TYPE_CHECKER.md).

### `interpreter` — ejecutar

Módulo Gradle `:interpreter` (`implementation` solo `common`). Recorre el `SyntaxProgram` y devuelve `Result<List<SideEffect>, RuntimeError>` (fail-fast, Result end-to-end). **Está cableado** en `ExecuteCode` (CLI `run`). `interpretCode` sigue cortando en el type-checker.

Dispatch por `node.name` (el nombre de regla de `grammar.config.v1.0.json`). `DefaultInterpreter` despacha statements; `DefaultExpressionSolver` despacha expresiones. Un handler declara `nodeNames`; no hay enum ni mapping aparte.

`println` es una **expresión** (`factor → call`), no un statement. Los efectos viajan en `EvalResult(value, sideEffects)` y se combinan de hijos a padres. `PrintlnHandler` emite `PrintEffect`.

Valores: `NumberValue(Double)`, `StringValue` (sin comillas), `UnitValue` (resultado de un call), `UninitializedValue` (`let` sin `=`). Números enteros se imprimen sin `.0` (`toPrintableString()`). Leer una no inicializada es `UninitializedVariable`.

Contexto: `InterpreterContext` inmutable copy-on-write, con `parent` y `childScope()`. `declareVariable` sombrea en el scope actual; `assignVariable` reconstruye la cadena dueña. V1 no tiene asignaciones sueltas ni bloques, así que `assignVariable` / `childScope` están listos y sin usar en el walk de statements.

Tipos en runtime: `DefaultTypeConfiguration` es una tabla **hardcodeada** (no lee `type-system.config.v1.0.json`). Tiene `number` con `+ - * /` y `string + string`. **No** tiene `string + number` (el type-checker sí). División por cero: guard explícito → `DivisionByZero`. El interpreter **no** chequea la anotación `TYPE` de un `let`: confía en el programa validado.

Ver [modules/INTERPRETER.md](modules/INTERPRETER.md).

### `linter` — estilo / SCA

Reglas de estilo sobre el árbol. Módulo Gradle `:linter`, cableado en `LintProgram`. No es type-check.

Ver [modules/LINTER.md](modules/LINTER.md).

### `formatter` — pretty-print

Módulo Gradle `:formatter`. Recibe configs ya parseadas + `SyntaxProgram` y produce texto canónico (`format` → `Result`) o un `Report` de mismatches (`check`). No lee archivos. Strategy: `addChar(point, char)` → `Gap` (newlines se suman entre AFTER+BEFORE, máx. un espacio **por hueco**; el cap no es una rule). `WalkState.indentLevel` entra en `Gap.render` (v1 = 0). `GrammarWalker` consume hijos de un `SeqRule` en orden, no por nombre. Rules de lenguaje (operadores, `;`+newline, `let`+espacio) y de usuario (`:` / `=` / newlines antes de `println`, con defaults). Reconstruye `let` / `:` / `=` / `;` / parens. Application: `FormatCode` / `CheckFormat` (carga JSON/YAML).

Ver [modules/FORMATTER.md](modules/FORMATTER.md).

### `infrastructure` — I/O y serializers

Único módulo con kotlinx.serialization. Lee JSON/YAML y archivos `.ps`. **No** es el CLI: el composition root vive en `:cli`.

- `JSONLanguageConfigReader` / `JSONGrammarConfigReader` / `JSONTypeSystemConfigReader` / `JSONFormatterLanguageConfigReader` / `JSONFormatterRulesConfigReader` / `YAMLFormatterRulesConfigReader` / `JSONLinterConfigReader` implementan los ports de `common`
- Serializers **surrogate** en `serializer/config`: el dominio no lleva `@Serializable`. `LanguageConfigSerializer` + `TokenRuleSerializer` (discrimina `type: exact|regex`)
- Discriminación de `GrammarRule` por **clave JSON** (`or`, `seq`, `left`, `atom`, `repeat`, `optional`), no por campo `type`
- Resources: `language.config.v1.0.json` / `v1.1.json` (hoy iguales), igual para grammar, type-system, formatter-language, linter; `formatter-user-defaults.json` sin versión

Ver [modules/INFRASTRUCTURE.md](modules/INFRASTRUCTURE.md).

### `application` — orquestación

- `InterpretCode.interpretCode(...)` — lex + parse + type-check → `Report`
- `ExecuteCode.execute(...)` — lo anterior + interpreter → `Report<Unit, Error>`
- `FormatCode` / `CheckFormat` / `LintProgram` — reciben `CodeReader` + configs/formatter ya armados
- `LoadFormatter` arma el `Formatter` a partir de configs parseadas (no lee archivos). Lo llama el CLI al arrancar
- No hay `Main.kt` acá: el CLI vive en `:cli`
- Tests de integración con archivos `.ps` y un DSL `assertAst { node(...) }`

Ver [modules/APPLICATION.md](modules/APPLICATION.md).

### `cli` — composition root

Plugin `application`, `mainClass = printscript.cli.MainKt`. Carga configs (readers de infrastructure + `LoadFormatter`), registra subcomandos `run` / `lint` / `check` / `format` / `typecheck`, cada uno llama al use case y presenta stdout/stderr.

Ver [modules/CLI.md](modules/CLI.md).

### `build-logic` — calidad del repo (no del lenguaje)

Included build. Convention plugin `printscript.quality`: ktlint (`ktlintCheck` / `ktlintFormat`) + detekt, y `installGitHooks` en el root. Se aplica al root y a los subproyectos desde el `build.gradle.kts` raíz. **No** es el linter/formatter de PrintScript.

Ver [modules/BUILD_LOGIC.md](modules/BUILD_LOGIC.md).

---

## Principios de diseño (no romperlos)

1. **Dominio sin serialización.** Clases de `common` no tienen `@Serializable`. JSON vive en `infrastructure` con `KSerializer` + surrogate.
2. **Tokens genéricos.** `Token.type: String`. El enum `TokenType` es leftover; no lo uses en código nuevo.
3. **Parser genérico.** El parser no conoce `let` ni `println`. Esas palabras están en los JSON. Un handler nuevo = data class en `common` + serializer + `RuleHandler` + registro.
4. **Árbol de sintaxis.** El pipeline camina `SyntaxNode` (`child` / `find` / `value`). No hay AST tipado aparte.
5. **Errores por capa.** Lexer tira `Error` / `IllegalStateException`. Parser tira `ParseException`. Type-checker acumula en `Report` / `Result` (su `TypeError` data class, no lanza). `interpretCode` / `formatCode` / `checkFormat` **no** lanzan: devuelven `Report` / `Result`. Interpreter reporta `Result.Err(RuntimeError)` y no lanza.
6. **Streaming.** Ni lexer ni parser cargan el programa entero de una: caracteres → tokens on demand → un statement por llamada.

El interpreter despacha por nombres de regla (`node.name`) y registra `CallHandler`s (`println`). Extenderlo es registrar executor/evaluator/handler, no tocar el motor de dispatch ni un enum.

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
| `STRING_LITERAL` | `"hola"` o `'hola'` (con comillas) | sí |
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
| `interpretCode` no ejecuta | `InterpretCode.kt` | El CLI `run` usa `ExecuteCode`; `interpretCode` sigue siendo solo type-check |
| Lexer/parser tiran excepciones | lexer, parser | El CLI las atrapa y imprime `ERROR`; el resto del pipeline usa `Result`/`Report` |
| `string + number` diverge | type-system JSON vs `DefaultTypeConfiguration` | El type-checker acepta `"a" + 1`; el interpreter responde `InvalidOperands` |
| Tabla de ops del interpreter hardcodeada | `interpreter/.../DefaultTypeConfiguration.kt` | No comparte `type-system.config.v1.0.json` con el type-checker |
| `partial` de números en el JSON | `language.config.v1.0.json` (`^[0-9]`) | `1.5` no tokeniza con el resource; strings `"..."` y `'...'` sí |
| Dos `TypeError` | `common/.../error/TypeError.kt` vs `type-checker/.../TypeError.kt` | El pipeline de application usa el data class del módulo; el sealed de common lo usa el interpreter (variantes compartidas) |
| `TokenType` enum | `common/.../TokenType.kt` | No lo usa nadie |
| `TokenRegistry` | `lexer/.../TokenRegistry.kt` | No lo usa el `TokenStream` |
| `repeat` listo, no usado en v1 | grammar + `RepeatRuleHandler` | Sirve para `if` / bloques |
| `COMMA` tokenizado, no parseado | language config | Pensado para args múltiples |

---

## Cómo extender (recetas cortas)

Versión de lenguaje / kits: `LanguageCatalog` en `:application` — detalle en [modules/APPLICATION.md](modules/APPLICATION.md).

### Nuevo token (keyword, operador, literal)

1. Agregar regla en `language.config.v1.0.json` (y en los `LanguageConfig` de test: `lexer`/`application` `PrintScriptLanguage`, `PsSupport` del interpreter) + un caso en `PrintScriptLexerTest`.
2. Poner la categoría en `order` **al principio si tiene que ganar** (el resolver usa índice mínimo). Keywords tienen que estar **antes** de identifiers.
3. Si el parser lo consume: usarlo en `grammar.config.v1.0.json` (`"LET"` o `{ "capture": "ID" }`).
4. Ver [LANGUAGE_CONFIG.md](configs/LANGUAGE_CONFIG.md).

### Nueva producción de gramática

1. Editar `grammar.config.v1.0.json`.
2. Si alcanza con `or`/`seq`/`left`/`atom`/`repeat`/`optional`, no hay código Kotlin nuevo.
3. Tests: `parser` (handlers + `ParserTest`) y un `.ps` en application si es de integración.
4. Ver [GRAMMAR_CONFIG.md](configs/GRAMMAR_CONFIG.md).

### Nuevo **tipo** de regla de gramática (no solo una producción)

1. Data class en `common` implementando `GrammarRule` (`references()`).
2. Serializer surrogate en `infrastructure` + rama en `GrammarRuleSerializer.select`.
3. `RuleHandler` en `parser` + entrada en `RuleHandlers.defaults()`.
4. Tests de handler y de deserialización.

### Nuevo tipo u operador (sin kind nuevo)

1. Editar `type-system.config.v1.0.json` (`types`, `literals`, `operations`) — eso alimenta al **type-checker**.
2. Si el lenguaje lo escribe: token en `language.config.v1.0.json` + producción en grammar.
3. Si el interpreter tiene que ejecutarlo: regla en `DefaultTypeConfiguration` (hoy no lee el JSON).
4. Ver [TYPE_SYSTEM_CONFIG.md](configs/TYPE_SYSTEM_CONFIG.md).

### Type-checker (kind nuevo)

Ya camina `SyntaxNode` y está cableado después del parser. Nuevo *kind* → handler en `:type-checker`. Detalle en [modules/TYPE_CHECKER.md](modules/TYPE_CHECKER.md).

### Interpreter (construcción nueva)

Módulo existente. Nueva construcción → executor/evaluator con `nodeNames` = regla del JSON, registrado en `DefaultInterpreterFactory`. Detalle en [modules/INTERPRETER.md](modules/INTERPRETER.md). Cablearlo: dependencia en `application` + llamada después del type-check en `InterpretCode.kt`.

### Formatter (rule nueva)

1. Si entra en space/newline: `rules` o `userBindings` en `formatter-language.v1.0.json`. El YAML de usuario sigue siendo `type` + `enabled`/`count`.
2. Si no: `FormatRule` + `FormatRuleFactory` en `:formatter`.
3. Tests de `format` y `check`. Detalle: [modules/FORMATTER.md](modules/FORMATTER.md), [configs/FORMATTER_CONFIG.md](configs/FORMATTER_CONFIG.md).
4. Bloques / `if`: no hace falta otro combiner. Gramática + `newline-after`/`newline-before` en el JSON, y en `emitSyntheticToken` subir/bajar `WalkState.indentLevel` alrededor de `{` `}`. Receta en [modules/FORMATTER.md](modules/FORMATTER.md).

### Linter

Módulo existente, cableado en `LintProgram`. Detalle: [modules/LINTER.md](modules/LINTER.md).

### CLI

Nuevo subcomando: clase en `cli/command/` que carga el `.ps`, llama al use case y presenta el resultado. Registrar en `PrintScriptCli.create()`. Detalle: [modules/CLI.md](modules/CLI.md).

---

## Tests — qué corre qué

| Módulo | Qué cubren |
|---|---|
| `lexer` | PrintScript v1 (kinds, statements, peek, errores, locations), `RuleEvaluator`, `RuleDrawResolver`, `TokenFactory`; `order` = keywords first, igual que el JSON |
| `parser` | Cada handler, gramática PrintScript completa (precedencia, parens, errores), `Grammar` validation, `SyntaxNode` |
| `type-checker` | Scope, resolver (literales, binarios, permutación), `TypeChecker` (match/mismatch/redeclare), `check` vs `checkStrict` |
| `interpreter` | contexto (scope/shadow/assign), evaluators (literales/binarios/calls/div-cero), executors, integración lex+parse+interpret con `SideEffect` |
| `formatter` | `format`/`check` de `1+2`, gap/indent render, walker con dos hijos del mismo nombre, loader (bindings + defaults JSON / type desconocido / `count` inválido), JSON real |
| `infrastructure` | `JSONLanguageConfigReader` / `JSONGrammarConfigReader` / `JSONTypeSystemConfigReader` contra el resource real; readers del formatter y del linter |
| `common` | `Result`/`Report`, `Grammar`/`SyntaxNode`/`SyntaxProgram`, `TokenLexemes`, configs (`TypeSystem` / formatter / linter), sealed errors, `PrintEffect` |
| `application` | `.ps` end-to-end lex+parse+type-check (`Report`); `ExecuteCode` (prints); format/check de `1+2;` y `let x:number=1;` (`Result`/`Report`) |
| `cli` | pipeline real vía `PrintScriptCli.create()` (run/format/check/typecheck + `ERROR` de parse + `--version` / help) |

Correr: `./gradlew test` (o `:lexer:test`, etc.). CI: `.github/workflows/ci.yml` corre wrapper / `ktlintCheck` / `detekt` en paralelo, y `assemble` + `test` en el mismo job (un compilado).

---

## Invariantes que un agente no debe violar

- No pongas `@Serializable` en `common`.
- No hagas que el parser conozca nombres de keyword; eso va en JSON.
- No asumas que `TokenType.IDENTIFIER` existe en runtime: el lexer emite `"ID"`.
- `LanguageConfig.order[0]` es la categoría **más** prioritaria (keywords primero, identifiers último).
- `Grammar(...)` explota si `start` o una referencia no existen; no construyas gramáticas a mano sin pasar por eso.
- `DefaultParser` cachea el `TokenSource` por identidad del `Lexer`: no reutilices un parser con **otro** lexer sin un parser nuevo (o el bind se queda corto si es el mismo objeto).
- Locations: `FileCodeReader` arranca en `(1,1)`; el `MockReader` de tests del lexer arranca línea `0`. No compares locations entre esos dos mundos.
- No aplanes `expression`/`term` de un solo hijo: el interpreter y los tests de application dependen del wrap de `LeftRule`.
- `DefaultInterpreter` no lanza en construcción. Kind mapeado sin executor ni evaluator → `Result.Err(UnresolvableExpression)` al interpretar; duplicados de kind se quedan con el último handler.

---

## Por dónde empezar según la tarea

| Tarea | Leer primero | Tocar |
|---|---|---|
| Cambiar qué tokens existen | LANGUAGE_CONFIG + lexer + infrastructure JSON | `language.config.v1.0.json`, tests de lexer/application/interpreter |
| Cambiar sintaxis | GRAMMAR_CONFIG + parser | `grammar.config.v1.0.json`, `ParserTest`, ejemplos `.ps` |
| Nuevo combinador de gramática | parser + infrastructure serializers + common domain | 3 módulos a la vez |
| Tipos / variables no declaradas | TYPE_CHECKER + TYPE_SYSTEM_CONFIG | `:type-checker` + `type-system.config.v1.0.json` |
| Ejecutar el programa | INTERPRETER | ya existe `:interpreter`; cablear en `application/InterpretCode.kt` |
| Nueva construcción a ejecutar | INTERPRETER | executor/evaluator con `nodeNames` + registro en factory |
| Reglas de estilo | LINTER | módulo a futuro |
| Pretty-print / bloques | FORMATTER | gramática de `if`/`{` + bump de `indentLevel` en `emitSyntheticToken` (el render ya existe) |
| Lint/format del Kotlin del repo | BUILD_LOGIC | `build-logic` / `printscript.quality` |
| CLI / correr un archivo | CLI + INFRASTRUCTURE | `./gradlew ps-run examples/hello.ps` |
| Nuevo subcomando | CLI + INFRASTRUCTURE | comando en `:cli` (use case) + `*Effects` en infra + factory |
| Leer un `.ps` de otro lado (stdin, string) | common `CodeReader` + infrastructure | nueva impl de `CodeReader` |
