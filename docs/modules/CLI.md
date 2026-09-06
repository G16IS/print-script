# Módulo `cli`

Dependencias: `common` + Clikt 5.1.0 (`api`) + `:application` (use cases) + `:infrastructure` (readers JSON/YAML + `FileCodeReader`) + `:formatter` / `:type-checker` (tipos de presentación). Plugin `application` (`mainClass = printscript.cli.MainKt`).

Composition root: carga las configs, llama a los use cases, imprime el resultado. No reimplementa lex/parse/format/lint.

---

## Cuándo tocarlo

- Nuevo subcomando que llama a un use case (`cli/command/` + registro en `PrintScriptCli.create()`)
- Flag compartido en el root (`--version`) o `emit(CommandResult)`
- Cómo se presentan prints / OK / errores (`Present.kt`, `ErrorFormatting.kt`)
- Cómo se cargan los JSON/YAML (`PrintScriptConfigs.load`)
- **No** para la semántica de format/lint/run: use cases de application
- **No** para serializers JSON: `infrastructure`

---

## API pública

```kotlin
abstract class SourceFileCommand(
    name: String,
    helpText: String,
) : CliktCommand(name)

class PrintScriptCli(vararg commands: CliktCommand) : CliktCommand(name = "printscript") {
    companion object {
        fun create(configs: PrintScriptConfigs = PrintScriptConfigs.load()): PrintScriptCli
    }
    fun runCli(args: Array<String>)
    fun capture(args: Array<String>): CliExecution
}
```

`PrintScriptConfigs.load()` lee los JSON del classpath de `:infrastructure` y el YAML de usuario (`.printscript/formatter.yml` si existe). Arma el `Formatter` con `LoadFormatter`.

Cada comando de producción llama al use case y pasa el resultado por `presentRun` / `presentFormat` / `presentReport`. Eso atrapa excepciones de lex/parse (`ERROR`), ejecuta `PrintEffect`s, imprime fuente formateada o `OK` / errores.

---

## Mapa de archivos

```
cli/src/main/kotlin/printscript/cli/
  Main.kt                        entrypoint: PrintScriptCli.create().runCli(args)
  PrintScriptCli.kt              root + --version + create()
  PrintScriptConfigs.kt          bag + load() (readers + LoadFormatter)
  SourceFileCommand.kt           argumento `file` + emit
  Present.kt                     Result/Report → CommandResult
  ErrorFormatting.kt             mensaje + (line:col-line:col)
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

Los registra `PrintScriptCli.create()`.

| Comando | Use case | Salida |
|---|---|---|
| `run <file>` | `ExecuteCode` | prints, o errores de tipo/runtime |
| `lint <file>` | `LintProgram` | `OK` o errores |
| `check <file>` | `CheckFormat` | `OK` o mismatches |
| `format <file>` | `FormatCode` | fuente formateada |
| `typecheck <file>` | `InterpretCode` | `OK` o errores de tipo |

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

`PrintScriptCliTest` arma un `.ps` temporal y llama `PrintScriptCli.create().capture(...)`: cubre `run` / `typecheck` / `format` / `check`, parse inválido → `ERROR`, `--version 2.0` → `ERROR`, y help si no hay subcomando.
