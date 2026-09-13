package printscript.cli

import printscript.SideEffect
import printscript.error.Error
import printscript.io.SideEffectManager
import printscript.usecases.ExecutionFailure
import printscript.util.Report
import printscript.util.Result
import printscript.util.fold

internal fun presentRun(block: () -> Result<List<SideEffect>, ExecutionFailure>): CommandResult =
    catching {
        block().fold(
            onOk = { effects ->
                effects.forEach { effect -> SideEffectManager().handle(effect) }

                return@fold CommandResult.Ok
            },
            onErr = { failure ->
                when (failure) {
                    is ExecutionFailure.Types ->
                        CommandResult.Failed(failure.errors.map { formatError(it) })
                    is ExecutionFailure.Runtime ->
                        CommandResult.Failed(listOf(formatRuntimeError(failure.error)))
                }
            },
        )
    }

internal fun presentFormat(block: () -> Result<String, Error>): CommandResult =
    catching {
        block().fold(
            onOk = { CommandResult.Output(it) },
            onErr = { CommandResult.Failed(listOf(formatError(it))) },
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
