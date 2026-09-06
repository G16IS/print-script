package printscript.cli

import printscript.PrintEffect
import printscript.SideEffect
import printscript.error.FormatError
import printscript.util.Report
import printscript.util.Result
import printscript.util.fold
import usecases.ExecutionFailure

internal fun presentRun(block: () -> Result<List<SideEffect>, ExecutionFailure>): CommandResult =
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

internal fun presentFormat(block: () -> Result<String, FormatError>): CommandResult =
    catching {
        block().fold(
            onOk = { CommandResult.Output(it) },
            onErr = { CommandResult.Failed(listOf(formatFormatError(it))) },
        )
    }

internal fun <E> presentReport(
    block: () -> Report<*, E>,
    format: (E) -> String,
): CommandResult =
    catching {
        val report = block()
        if (report.isOk) {
            CommandResult.Ok
        } else {
            CommandResult.Failed(report.errors.map(format))
        }
    }

private fun catching(block: () -> CommandResult): CommandResult =
    try {
        block()
    } catch (_: Exception) {
        CommandResult.Failed(listOf("ERROR"))
    }
