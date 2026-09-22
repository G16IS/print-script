package printscript.cli

import printscript.error.Error
import printscript.error.formatError
import printscript.util.Report
import printscript.util.Result
import printscript.util.fold

internal fun presentRun(block: () -> Result<Unit, Error>): CommandResult =
    catching {
        block().fold(
            onOk = { CommandResult.Ok },
            onErr = { CommandResult.Failed(listOf(formatError(it))) },
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
