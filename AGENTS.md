# AGENTS.md

Guía operativa para trabajar en este repo. El detalle de diseño está en [`docs/CONTEXT.md`](docs/CONTEXT.md). Si este archivo y un `.md` de `docs/` no coinciden, **gana el código**.

`docs/CONSIGNA.md` es la consigna del TP: no modificarlo.

## Stack

| Pieza | Versión / dato | Fuente |
|---|---|---|
| Lenguaje | Kotlin **2.4.10** | `gradle/libs.versions.toml` |
| JVM | **21** (`kotlin.jvmToolchain(21)` en cada módulo y en `buildSrc`) | `*/build.gradle.kts`, CI Temurin 21 |
| Build | Gradle **9.6.1** (wrapper), Kotlin DSL, monorepo | `gradle/wrapper/gradle-wrapper.properties` |
| Root | `print-script-g16` | `settings.gradle.kts` |
| Tests | JUnit Platform (`useJUnitPlatform()` en todos los módulos) | `*/build.gradle.kts` |
| Serialización | kotlinx.serialization **1.9.0** + kaml **0.73.0** — **solo** en `:infrastructure` | `gradle/libs.versions.toml`, `infrastructure/build.gradle.kts` |
| Calidad Kotlin | ktlint plugin **14.2.0**, detekt **2.0.0-alpha.6** | plugin `com.g16is.conventions.quality` 1.0.0 (GitHub Packages) |
| JDK en CI | Temurin 21 + cache de Gradle | `.github/workflows/*.yml` |
| Toolchains | Foojay resolver `1.0.0` (descarga JDKs) | `settings.gradle.kts` |

No hay frontend, HTTP, ni plugin `application`. No hay `Main.kt`. No hay `CHANGELOG`.

JUnit no está unificado:

- Mayoría de módulos: BOM `org.junit:junit-bom:5.10.0` + `junit-jupiter`.
- `:application`: `junit-jupiter:6.0.1` **y** `kotlin("test")`.
- `:common` y `:type-checker`: solo `kotlin("test")`.
- `:linter`: JUnit 5.10.0 **y** `kotlin("test")`.

## Comandos

Hace falta **JDK 21**. El wrapper es `./gradlew`. CI le pone `chmod +x` al wrapper.

```bash
# tests — todos los módulos (CI: .github/workflows/tests.yml; corre en todo push/PR)
./gradlew test --continue --no-daemon

# un módulo (documentado en docs/CONTEXT.md)
./gradlew :lexer:test
./gradlew :parser:test
./gradlew :type-checker:test
./gradlew :interpreter:test
./gradlew :formatter:test
./gradlew :linter:test
./gradlew :infrastructure:test
./gradlew :common:test
./gradlew :application:test

# estilo Kotlin — check, no reescribe (CI: .github/workflows/format.yml; solo main + PRs a main)
./gradlew ktlintCheck --continue --no-daemon

# estilo Kotlin — reescribe (pre-commit / docs/modules/BUILD_LOGIC.md; no está en CI)
./gradlew ktlintFormat

# análisis estático (CI: .github/workflows/lint.yml; solo main + PRs a main)
./gradlew detekt --continue --no-daemon

# git hooks (pre-commit corre ktlintCheck + detekt si hay .kt/.kts staged)
./gradlew installGitHooks
```

Pre-commit / post-commit los instala el plugin `com.g16is.conventions.quality` (`./gradlew installGitHooks` copia los scripts a `hooks/` y setea `core.hooksPath`). Bypass: `git commit --no-verify`. Auto-format sugerido por el hook: `./gradlew ktlintFormat`.

Configuration cache está prendido: `org.gradle.configuration-cache=true` en `gradle.properties`.

**TODO:** el repo no documenta ni usa un filtro de un solo test. Cada módulo tiene `useJUnitPlatform()`, así que Gradle acepta `--tests`, pero no hay un ejemplo en CI ni en docs. No asumas una sintaxis de método (los nombres van entre backticks) sin probarla.

## Estructura

Pipeline de PrintScript (streaming: caracteres → tokens on demand → un statement por llamada):

```
.ps → infrastructure (FileCodeReader + JSON)
    → lexer → parser → type-checker     ← interpretCode hoy corta acá
                    ↘ formatter         ← FormatCode / CheckFormat
                    ↘ linter            ← LintProgram (existe; nadie lo llama fuera de application)
                    ↘ interpreter       ← módulo listo; application no depende de él
```

`settings.gradle.kts` incluye: `common`, `lexer`, `infrastructure`, `parser`, `type-checker`, `application`, `interpreter`, `linter`, `formatter`, `cli`. Los convention plugins `com.g16is.conventions.*` se resuelven desde GitHub Packages. `buildSrc` tiene `PrintScriptExec`.

### Módulos

| Módulo | Rol | Dependencias de producción |
|---|---|---|
| `:common` | Contratos: Token, Grammar, SyntaxNode, configs, `Result`/`Report`, errores, puertos de I/O. Sin serializers ni filesystem. | ninguna (solo JDK) |
| `:lexer` | Código → tokens. `Lexer.create`. `Token.type` es `String`. | `:common` |
| `:parser` | Tokens → `SyntaxProgram`. Parser genérico dirigido por `Grammar`. | `:common`, `:lexer` |
| `:type-checker` | Tipos y símbolos sobre el árbol. `check` acumula; `checkStrict` corta. | `:common` |
| `:interpreter` | `SyntaxProgram` → `Result<List<SideEffect>, RuntimeError>`. Fail-fast. | `:common` |
| `:formatter` | Pretty-print (`format` → `Result`) / check de whitespace (`check` → `Report`). | `:common` |
| `:linter` | Estilo estático sobre el árbol (`identifier-format`, `println-simple-argument`). | `:common` |
| `:infrastructure` | Único módulo con kotlinx.serialization. Readers JSON/YAML + `FileCodeReader` / `StringCodeReader`. Resources en `src/main/resources/`. | `:common` + serialization-json + kaml |
| `:application` | Casos de uso. Paquete de producción: `usecases`. | `:common`, `:lexer`, `:parser`, `:type-checker`, `:formatter`, `:linter`, `:infrastructure`. **No** `:interpreter`. |
| `buildSrc` | `PrintScriptExec` para tasks `ps-*`. Quality/coverage/publishing son `com.g16is.conventions.*` 1.0.0 desde Packages. | — |

Resources de lenguaje (classpath de infrastructure):

- `language.config.v1.0.json` / `language.config.v1.1.json` (hoy iguales)
- `grammar.config.v1.0.json` / `grammar.config.v1.1.json`
- `type-system.config.v1.0.json` / `type-system.config.v1.1.json`
- `formatter-language.v1.0.json` / `formatter-language.v1.1.json`
- `formatter-user-defaults.json`
- `linter.config.v1.0.json` / `linter.config.v1.1.json`

YAML opcional de usuario del formatter: `.printscript/formatter.yml` (constante `LoadFormatter.USER_YAML_PATH`). Si no existe, se usan los defaults JSON.

Tests extra (no son deps de producción):

- `:parser` / `:formatter` testImplementation `:infrastructure`
- `:interpreter` / `:linter` testImplementation `:lexer`, `:parser`, `:infrastructure`

Paquetes:

- Código reutilizable: `printscript.*`
- Application producción: `usecases`
- Application tests: `edu.austral.dissis`

## Convenciones

### Estilo Kotlin (ktlint + `.editorconfig`)

`.editorconfig` (root = true):

- UTF-8, LF, newline final, trim trailing whitespace
- indent: 4 espacios
- `*.{kt,kts}`: `max_line_length = 120`
- trailing commas permitidas (`ij_kotlin_allow_trailing_comma` y `…_on_call_site`)
- `ij_kotlin_imports_layout = *`

ktlint (plugin, sin `.ktlint.yml` ni versión del engine pinneada en el repo): `android = false`, `outputToConsole`, `verbose`, `coloredOutput`, `relative`. Lee el `.editorconfig`.

detekt: `buildUponDefaultConfig = true`, `allRules = false`, config `config/detekt/detekt.yml`. Override real: `ForbiddenComment` activo para `FIXME:` y `STOPSHIP:`. **No** lista `TODO:` — los `TODO` están permitidos. Reportes archivo (html/checkstyle/sarif/markdown) apagados; findings por consola. `jvmTarget = 21`.

### Diseño que el código y `docs/CONTEXT.md` piden no romper

1. **Dominio sin serialización.** Clases de `common` no llevan `@Serializable`. JSON vive en `infrastructure` con `KSerializer` + surrogate. `GrammarRule` se discrimina por **tag JSON** (`or`, `seq`, `left`, `atom`, `repeat`); `TokenRule` usa `type: "exact" | "regex"`.
2. **Tokens genéricos.** `Token.type: String` (`"LET"`, `"ID"`, `"NUMBER_LITERAL"`, `"EOF"`, …). El enum `TokenType` es leftover: no usarlo en código nuevo.
3. **Parser genérico.** No conoce `let` ni `println`; eso está en los JSON. Un combinador nuevo = data class en `common` + serializer + `RuleHandler` + registro en `RuleHandlers.defaults()`.
4. **Árbol.** El pipeline camina `SyntaxNode` (`name` = regla de gramática o `Token.type` capturado). `LeftRule` **siempre envuelve**: un literal suelto queda `expression → term → number`. No aplanar.
5. **Streaming.** Ni lexer ni parser cargan el programa entero de una.
6. **Prioridad léxica.** `LanguageConfig.order[0]` gana. En el JSON y en los `LanguageConfig` de test: keywords → types → operators → literals → identifiers. Si identifiers va primero, `let` se tokeniza como `ID`.

### Factories y casos de uso

Patrón repetido: interface (`Parser`, `Interpreter`, `Linter`, `Formatter`, `TypeChecker`) + `DefaultX` + `object DefaultXFactory` con `create(...)`. El lexer es la excepción: `Lexer.create(...)`; `DefaultLexerFactory` es `internal`.

Application: `object`s (`InterpretCode`, `FormatCode`, `CheckFormat`, `LintProgram`, `ParseProgram`, `LoadFormatter`). `ParseProgram` / `LoadFormatter` / `LintProgram` son `internal`.

### Manejo de errores (por capa; no está unificado)

| Capa | Qué hace |
|---|---|
| Lexer | No lanza en tokenización. `nextToken` / `peek` → `Result<Token, LexerError>` (`UnexpectedToken`, `UnexpectedEnfOfLine`, `MultipleRulesWithSamePriority`, `RuleNotFound`). EOF es `Token("EOF", …)`. |
| Parser | Lanza `ParseException` (sintaxis). |
| Type-checker | No lanza. `Report` / `Result` con **su propio** `printscript.typechecker.TypeError` (data class `message` + `location`), no el sealed de `common`. |
| Interpreter | `Result.Err(RuntimeError)` end-to-end (`map` / `flatMap`). Fail-fast. No lanza: wiring incompleto o nodo malformado también es `Err` (`UnresolvableExpression` / `UnrecognizedNode`). |
| Formatter | `format` → `Result<String, FormatError>`; `check` → `Report<Unit, FormatError>`. |
| Linter | Violaciones → `Report<SyntaxProgram, LintError>`. Config inválida en factory → `IllegalArgumentException` / `require`. |
| `interpretCode` | Devuelve el `Report` del type-checker. **No interpreta.** Hay `TODO` explícitos en `InterpretCode.kt`. |
| `formatCode` / `checkFormat` | No lanzan; `Result` / `Report`. |
| `ParseProgram.parse` | `Result<SyntaxProgram, Error>` (`LexerError` o `ParserError`). No lanza por el lexer. |

Jerarquía en `common` (`printscript.error`):

- `sealed interface Error`
- `TypeError` / `RuntimeError` / `FormatError` extienden `Error`
- `UndeclaredIdentifier`, `InvalidOperands`, `UnrecognizedNode` implementan **TypeError y RuntimeError**
- `LintError` **no** implementa `Error` (solo `message` + `location`)
- El formatter usa `UnrecognizedFormatNode` para no chocar con `UnrecognizedNode`

### Naming

- Módulos Gradle: kebab-case (`type-checker`).
- Clases: PascalCase. Implementaciones por defecto: prefijo `Default`.
- Tests: `FooTest.kt` junto al paquete de producción (application tests viven en `edu.austral.dissis`).
- Config JSON: `*.config.json` / `formatter-*.json` / `linter.config.v1.0.json`.
- Archivos de ejemplo PrintScript: `*.ps`.

`java.util.Optional` aparece en `Token.value` y en `CodeReader` (`read`/`peek`), no tipos nullable de Kotlin.

## Testing

Todos los módulos usan JUnit Platform. Los nombres de test son **oraciones en inglés entre backticks**, no `given/when/then` ni `testFoo`:

```kotlin
@Test
fun `first runtime error aborts execution with Err`() { … }
```

Estructura típica (arrange / act / assert, sin labels):

```kotlin
val program = PsSupport.parse("println(nope);")
val result = DefaultInterpreterFactory.create().interpret(InterpreterContext(), program)
assertTrue(result is Result.Err)
assertTrue((result as Result.Err).error is UndeclaredIdentifier)
```

Patrones vistos:

- Lexer: `@Nested` por categoría (`Keywords`, `Types`, …) + helpers `assertLex` / `tok` en `support/`.
- Parser: `MockLexer` + `PrintScriptGrammar` + `assertThrows<ParseException>`.
- Interpreter / formatter: helper privado `ok(result)` que casteá `Result.Ok` y extrae `value`.
- Application: archivos `.ps` en `application/src/test/resources/examples/` + DSL `assertAst { node("variable") { … } }`. `ParseExample.parse(...)` llama a `interpretCode` (lex + parse + type-check).
- Integración interpreter/linter: parsean strings escribiendo un temp `.ps` y usando lexer+parser reales + `grammar.config.v1.0.json` del classpath.
- Type-checker / common / parte de linter: `kotlin.test.*` (`assertTrue`, `assertIs`, …).
- Lexer / parser / interpreter / formatter / infrastructure / application: `org.junit.jupiter.api.*`.

Helpers de parse/language se **duplican** por módulo (`PrintScriptLanguage` en lexer y application, `PsSupport` en interpreter, `LinterPsSupport` en linter). No hay un test fixture compartido.

No hay tests de application para `LintProgram`.

## Gotchas

- **README y `docs/CONTEXT.md` están desactualizados en el linter.** El README dice que el linter no existe. El módulo `:linter` está incluido, tiene implementación + tests, y `application` depende de él (`LintProgram`). `docs/modules/LINTER.md` sí lo documenta como implementado. `docs/modules/APPLICATION.md` todavía lista deps sin `:linter` y no menciona `LintProgram.kt`.
- **`interpretCode` no ejecuta.** Corta en el type-checker y devuelve `Report<SyntaxProgram, TypeError>` (`type-checker.TypeError`). El interpreter solo lo ejercitan los tests de `:interpreter`.
- **`LintProgram` no tiene caller** fuera de su archivo. Cableado en application, sin test de caso de uso.
- **No hay CLI.** No hay `Main.kt`. No hay módulo `:cli` en `settings.gradle.kts`.
- **Dos `TypeError`.** `common/.../error/TypeError.kt` (sealed, lo usa el interpreter) vs `type-checker/.../TypeError.kt` (data class, lo usa `interpretCode`).
- **`string + number`.** El type-checker lo acepta (`type-system.config.v1.0.json`). `DefaultTypeConfiguration` del interpreter solo tiene `number⊕number` y `string+string` → `InvalidOperands` en runtime.
- **`partial` de literales en el JSON de producción es estrecho.** `language.config.v1.0.json`: números `^[0-9]`, strings `^"`. Eso no tokeniza `1.5` ni strings a mitad. Los tests de lexer/interpreter usan un `partial` más amplio (`^[0-9]+(\\.[0-9]*)?$`, `^"[^"]*$`). Application tests del lexer en código usan el `partial` estrecho de números (igual que el JSON).
- **Locations.** `FileCodeReader` arranca en `(1,1)`. `MockReader` (tests del lexer) y `StringCodeReader` cuentan `(line = 0, col = index)`. No compares locations entre esos mundos.
- **`DefaultParser` cachea el `TokenSource` por identidad del lexer.** No reutilices un parser con **otro** lexer sin crear un parser nuevo.
- **El interpreter despacha por `node.name`** (regla de `grammar.config.v1.0.json`). `CallEvaluator` registra `CallHandler`s (`PrintlnHandler`). La tabla de ops **no** lee `type-system.config.v1.0.json`.
- **`repeat` en el parser está listo** y la gramática v1 no lo usa (pensado para bloques/`if`). `COMMA` se tokeniza y no se parsea.
- **ktlint/detekt ≠ linter/formatter de PrintScript.** Lo primero es calidad del Kotlin (`com.g16is.conventions.quality`). Lo segundo son módulos del lenguaje.
- **CI de lint/format solo corre en `main` y PRs a `main`.** `tests.yml` corre en cualquier push/PR.
- **GitHub Packages para conventions.** `./gradlew` resuelve `com.g16is.conventions.*` desde `maven.pkg.github.com/G16IS/gradle-conventions`. Credenciales como el TCK: `gpr.user` / `gpr.key` en `gradle.properties` (gitignored), o env `USERNAME` / `TOKEN`. CI usa `GITHUB_ACTOR` / `GITHUB_TOKEN` con `packages: read`.

### Invariantes cortos (copiados del código, no de deseo)

- No pongas `@Serializable` en `common`.
- No hagas que el parser conozca keywords; van en JSON.
- No asumas `TokenType.IDENTIFIER`: el lexer emite `"ID"`.
- No aplanes `expression`/`term` de un solo hijo.
- `Grammar(...)` falla si `start` o una referencia no existen.
- `DefaultInterpreter` no lanza en construcción. `node.name` sin executor ni evaluator → `Result.Err(UnresolvableExpression)`. Handler duplicado: last-wins.

## Docs

| Para… | Archivo |
|---|---|
| Visión, pipeline, recetas de extensión | `docs/CONTEXT.md` |
| Un módulo | `docs/modules/<MODULO>.md` |
| Forma de los JSON | `docs/configs/` |
| Consigna del TP (no editar) | `docs/CONSIGNA.md` |
