package printscript.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument

abstract class SourceFileCommand(
    name: String,
    private val helpText: String,
) : CliktCommand(name) {
    protected val file by argument(help = "Path to the PrintScript source file")

    override fun help(context: Context): String = helpText
}

fun CliktCommand.emit(
    result: CommandResult,
    printOk: Boolean = true,
) {
    when (result) {
        CommandResult.Ok -> if (printOk) echo("OK")
        is CommandResult.Output -> echo(result.text, trailingNewline = false)
        is CommandResult.Failed -> {
            result.messages.forEach { echo(it, err = true) }
            throw ProgramResult(1)
        }
    }
}
