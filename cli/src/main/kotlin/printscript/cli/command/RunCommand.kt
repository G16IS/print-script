package printscript.cli.command

import printscript.cli.FileCommand

internal class RunCommand(
    command: FileCommand,
) : FileCliCommand(
        name = "run",
        helpText = "Execute a PrintScript file",
        command = command,
        printOk = false,
    )
