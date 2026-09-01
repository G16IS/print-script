package printscript.cli.command

import printscript.cli.FileCommand

internal class TypeCheckCommand(
    command: FileCommand,
) : FileCliCommand(
        name = "typecheck",
        helpText = "Type-check a PrintScript file",
        command = command,
        printOk = true,
    )
