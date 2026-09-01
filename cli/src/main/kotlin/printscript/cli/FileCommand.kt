package printscript.cli

fun interface FileCommand {
    fun execute(file: String): CommandResult
}
