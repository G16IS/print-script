package printscript.cli.command

import printscript.cli.SourceFileCommand
import printscript.cli.VersionKit
import printscript.cli.emit
import printscript.cli.presentFormat
import printscript.reader.FileCodeReader
import printscript.usecases.FormatCode

class FormatCommand : SourceFileCommand("format", "Format a PrintScript file and print it to stdout") {
    override fun run() {
        val kit = currentContext.findObject<VersionKit>() ?: error("Language kit not loaded")
        emit(
            presentFormat {
                FormatCode.formatCode(
                    kit.configs.lang,
                    kit.configs.grammar,
                    FileCodeReader(file),
                    kit.configs.formatter,
                    kit.langKit,
                )
            },
            printOk = false,
        )
    }
}
