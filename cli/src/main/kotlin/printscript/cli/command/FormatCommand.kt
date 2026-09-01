package printscript.cli.command

import printscript.cli.FileCommand

internal class FormatCommand(
    command: FileCommand,
) : FileCliCommand(
        name = "format",
        helpText = "Format a PrintScript file and print it to stdout",
        command = command,
        printOk = false,
    )
