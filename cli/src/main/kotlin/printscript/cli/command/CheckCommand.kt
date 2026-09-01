package printscript.cli.command

import printscript.cli.FileCommand

internal class CheckCommand(
    command: FileCommand,
) : FileCliCommand(
        name = "check",
        helpText = "Check that a PrintScript file matches the formatter",
        command = command,
        printOk = true,
    )
