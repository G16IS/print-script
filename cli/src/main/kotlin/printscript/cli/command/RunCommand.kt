package printscript.cli.command

import printscript.cli.SourceFileCommand
import printscript.cli.VersionKit
import printscript.cli.emit
import printscript.cli.presentRun
import printscript.reader.FileCodeReader
import printscript.usecases.ExecuteCode

class RunCommand : SourceFileCommand("run", "Execute a PrintScript file") {
    override fun run() {
        val kit = currentContext.findObject<VersionKit>() ?: error("Language kit not loaded")
        emit(
            presentRun {
                ExecuteCode.execute(
                    kit.configs.lang,
                    kit.configs.grammar,
                    kit.configs.typeSystem,
                    FileCodeReader(file),
                    kit.langKit,
                )
            },
            printOk = false,
        )
    }
}
