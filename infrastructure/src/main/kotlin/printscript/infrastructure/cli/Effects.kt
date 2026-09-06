package printscript.infrastructure.cli

import printscript.PrintEffect
import printscript.SideEffect
import printscript.cli.CommandEffects
import printscript.cli.CommandResult
import printscript.error.FormatError
import printscript.error.LintError
import printscript.syntax.SyntaxProgram
import printscript.typechecker.TypeError
import printscript.util.Report
import printscript.util.Result
import printscript.util.fold
import usecases.ExecutionFailure

object RunEffects : CommandEffects<Result<List<SideEffect>, ExecutionFailure>> {
    override fun handle(block: () -> Result<List<SideEffect>, ExecutionFailure>): CommandResult =
        catching {
            block().fold(
                onOk = { effects ->
                    CommandResult.Output(
                        effects.joinToString("") { effect ->
                            when (effect) {
                                is PrintEffect -> effect.text + "\n"
                            }
                        },
                    )
                },
                onErr = { failure ->
                    when (failure) {
                        is ExecutionFailure.Types ->
                            CommandResult.Failed(failure.errors.map { formatTypeError(it) })
                        is ExecutionFailure.Runtime ->
                            CommandResult.Failed(listOf(formatRuntimeError(failure.error)))
                    }
                },
            )
        }
}

object LintEffects : CommandEffects<Report<SyntaxProgram, LintError>> {
    override fun handle(block: () -> Report<SyntaxProgram, LintError>): CommandResult =
        catching { presentReport(block(), ::formatLintError) }
}

object CheckEffects : CommandEffects<Report<Unit, FormatError>> {
    override fun handle(block: () -> Report<Unit, FormatError>): CommandResult =
        catching { presentReport(block(), ::formatFormatError) }
}

object FormatEffects : CommandEffects<Result<String, FormatError>> {
    override fun handle(block: () -> Result<String, FormatError>): CommandResult =
        catching {
            block().fold(
                onOk = { CommandResult.Output(it) },
                onErr = { CommandResult.Failed(listOf(formatFormatError(it))) },
            )
        }
}

object TypeCheckEffects : CommandEffects<Report<SyntaxProgram, TypeError>> {
    override fun handle(block: () -> Report<SyntaxProgram, TypeError>): CommandResult =
        catching { presentReport(block(), ::formatTypeError) }
}

private fun <E> presentReport(
    report: Report<*, E>,
    format: (E) -> String,
): CommandResult =
    if (report.isOk) {
        CommandResult.Ok
    } else {
        CommandResult.Failed(report.errors.map(format))
    }

private fun catching(block: () -> CommandResult): CommandResult =
    try {
        block()
    } catch (_: Exception) {
        CommandResult.Failed(listOf("ERROR"))
    }
