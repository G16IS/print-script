package printscript.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.arguments.argument
import printscript.cli.CommandResult
import printscript.cli.FileCommand

internal abstract class FileCliCommand(
    name: String,
    private val helpText: String,
    private val command: FileCommand,
    private val printOk: Boolean,
) : CliktCommand(name) {
    private val file by argument(help = "Path to the PrintScript source file")

    override fun help(context: Context): String = helpText

    override fun run() {
        when (val result = command.execute(file)) {
            CommandResult.Ok -> if (printOk) echo("OK")
            is CommandResult.Output -> echo(result.text, trailingNewline = false)
            is CommandResult.Failed -> {
                result.messages.forEach { echo(it, err = true) }
                throw ProgramResult(1)
            }
        }
    }
}
