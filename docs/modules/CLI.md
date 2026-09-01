# Módulo `cli`

Dependencias: `common` + Clikt 5.1.0 (`api`, para que `:infrastructure` vea `CliktCommand`). No depende de `:application` ni de `:infrastructure`.

Fachada de línea de comandos: parsea args y despacha a handlers inyectados (`FileCommand`). No orquesta el pipeline, no lee JSON, no conoce lexer/parser. El composition root vive en `:infrastructure` (`PrintScriptRuntime` + `Main.kt`).

---

## Cuándo tocarlo

- Nuevo subcomando o flag de Clikt
- Cambiar `CommandResult` / cómo se imprime OK vs output vs errores
- Tests de parsing de args (handlers fake)
- **No** para cablear un caso de uso: eso es `PrintScriptRuntime` en infrastructure
- **No** para la semántica de format/lint/run: esos son los use cases de application

---

## API pública

```kotlin
fun interface FileCommand {
    fun execute(file: String): CommandResult
}

sealed interface CommandResult {
    data object Ok : CommandResult
    data class Output(val text: String) : CommandResult
    data class Failed(val messages: List<String>) : CommandResult
}

class PrintScriptCli(
    run: FileCommand,
    lint: FileCommand,
    check: FileCommand,
    format: FileCommand,
    typecheck: FileCommand,
) : CliktCommand(name = "printscript") {
    fun runCli(args: Array<String>)
    fun capture(args: Array<String>): CliExecution
}
```

Cada subcomando es un `CliktCommand` (Command pattern) que recibe un `FileCommand`. El root solo registra hijos y valida `--version` (default `1.0`; otra cosa → `ERROR` y exit 1).

`runCli` llama a `CliktCommand.main` (extension de Clikt 5). `capture` es para tests (`command.test(...)`).

---

## Mapa de archivos

```
cli/src/main/kotlin/printscript/cli/
  PrintScriptCli.kt              root + --version + subcommands
  FileCommand.kt                 fun interface del handler
  CommandResult.kt               Ok / Output / Failed
  CliExecution.kt                status + stdout + stderr (tests / runtime)
  command/
    FileCliCommand.kt            argumento `file`, echo, ProgramResult(1)
    RunCommand.kt
    LintCommand.kt
    CheckCommand.kt
    FormatCommand.kt
    TypeCheckCommand.kt
```

---

## Subcomandos

| Comando | Handler (lo inyecta infra) | Éxito |
|---|---|---|
| `run <file>` | `ExecuteCode` | stdout = `PrintEffect`s, un valor por línea |
| `lint <file>` | `LintProgram` | `OK` |
| `check <file>` | `CheckFormat` | `OK` |
| `format <file>` | `FormatCode` | fuente formateada a stdout (no pisa el archivo) |
| `typecheck <file>` | `InterpretCode` | `OK` |

Errores de `Result`/`Report` (tipos, lint, format, runtime) se imprimen en stderr con `mensaje (line:col-line:col)`. Lexer/parser todavía tiran: el runtime los atrapa y imprime `ERROR`. Exit 0 / 1.

---

## Cómo se corre

Desde la raíz del repo:

```
./gradlew ps-run examples/hello.ps
./gradlew ps-lint examples/hello.ps
./gradlew ps-check examples/hello.ps
./gradlew ps-format examples/hello.ps
./gradlew ps-typecheck examples/hello.ps
```

Gradle normalmente interpreta el path como otra task: `settings.gradle.kts` lo reescribe a `-Pfile=…`. Equivalente: `./gradlew ps-run --args="examples/hello.ps"`.

Clikt en sí no registra tasks. Las `ps-*` viven en el `build.gradle.kts` raíz (`PrintScriptExec`) y arrancan el `main` de `:infrastructure`.

---

## Tests

`PrintScriptCliTest` usa `capture` con `FileCommand` fake: no pega al pipeline. Cubre output de `run`/`format`, `OK` de lint, Failed → stderr + exit 1, `--version 2.0` → `ERROR`, help si no hay subcomando.
