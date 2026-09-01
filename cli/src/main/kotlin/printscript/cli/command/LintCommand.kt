package printscript.cli.command

import printscript.cli.FileCommand

internal class LintCommand(
    command: FileCommand,
) : FileCliCommand(
        name = "lint",
        helpText = "Lint a PrintScript file",
        command = command,
        printOk = true,
    )
