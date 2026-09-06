# Módulo `cli`

Dependencias: `common` + Clikt 5.1.0 (`api`) + `:application` (use cases) + `:formatter` / `:type-checker` (tipos que filtran las firmas). No depende de `:infrastructure`.

Comandos: parsean el path, llaman al use case, **devuelven** el resultado. No leen JSON, no imprimen side effects, no formatean errores. Eso lo inyecta infrastructure (`SourceFiles`, `CommandEffects`).

---

## Cuándo tocarlo

- Nuevo subcomando que llama a un use case (`cli/command/`)
- Flag compartido en el root (`--version`) o `emit(CommandResult)`
- Tests de parsing de args (handlers fake vía `FileCliCommand`)
- **No** para ejecutar prints / OK / errores: eso es `infrastructure/cli/Effects.kt`
- **No** para leer el `.ps` o los JSON: `infrastructure/cli` (`FileSources` / `DefaultConfigFactory`)
- **No** para la semántica de format/lint/run: use cases de application

---

## API pública

```kotlin
interface SourceFiles {
    fun reader(path: String): CodeReader
    fun text(path: String): String
}

fun interface CommandEffects<T> {
    fun handle(block: () -> T): CommandResult
}

abstract class SourceFileCommand(
    name: String,
    helpText: String,
    printOk: Boolean = true,
) : CliktCommand(name)

class FileCliCommand(...) : SourceFileCommand  // tests / comandos genéricos

class PrintScriptCli(vararg commands: CliktCommand) : CliktCommand(name = "printscript") {
    fun runCli(args: Array<String>)
    fun capture(args: Array<String>): CliExecution
}
```

Cada comando de producción (`RunCommand`, `LintCommand`, …) llama al use case adentro de `effects.handle { ... }`. Infra atrapa excepciones, ejecuta `PrintEffect`s, imprime fuente formateada o `OK` / errores.

---

## Mapa de archivos

```
cli/src/main/kotlin/printscript/cli/
  PrintScriptCli.kt              root + --version + subcommands
  FileCliCommand.kt              SourceFileCommand + FileCliCommand + emit
  SourceFiles.kt                 puertos SourceFiles + CommandEffects
  FileCommand.kt                 fun interface (tests / FileCliCommand)
  CommandResult.kt               presentación: Ok / Output / Failed
  CliExecution.kt                status + stdout + stderr
  command/
    RunCommand.kt                ExecuteCode → List<SideEffect>
    LintCommand.kt               LintProgram → Report
    CheckCommand.kt              CheckFormat → Report
    FormatCommand.kt             FormatCode → String
    TypeCheckCommand.kt          InterpretCode → Report
```

---

## Subcomandos

Los registra `CommandFactories.defaults()` en `infrastructure/cli`.

| Comando | Use case | Resultado que ve infra |
|---|---|---|
| `run <file>` | `ExecuteCode` | `List<SideEffect>` o `ExecutionFailure` |
| `lint <file>` | `LintProgram` | `Report` ok/err |
| `check <file>` | `CheckFormat` | `Report` ok/err |
| `format <file>` | `FormatCode` | fuente formateada |
| `typecheck <file>` | `InterpretCode` | `Report` ok/err |

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

---

## Tests

`PrintScriptCliTest` usa `FileCliCommand` + `FileCommand` fake: no pega al pipeline. Cubre output de `run`/`format`, `OK` de lint, Failed → stderr + exit 1, `--version 2.0` → `ERROR`, help si no hay subcomando, y un subcomando extra sin tocar el root.
