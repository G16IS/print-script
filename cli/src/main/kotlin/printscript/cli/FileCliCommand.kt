package printscript.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument

abstract class SourceFileCommand(
    name: String,
    private val helpText: String,
    protected val printOk: Boolean = true,
) : CliktCommand(name) {
    protected val file by argument(help = "Path to the PrintScript source file")

    override fun help(context: Context): String = helpText
}

class FileCliCommand(
    name: String,
    helpText: String,
    printOk: Boolean = true,
    private val command: FileCommand,
) : SourceFileCommand(name, helpText, printOk) {
    override fun run() = emit(command.execute(file), printOk)
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
