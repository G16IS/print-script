package printscript.cli.command

import java.nio.file.Files
import java.nio.file.Path
import printscript.cli.SourceFileCommand
import printscript.cli.VersionKit
import printscript.cli.emit
import printscript.cli.formatError
import printscript.cli.presentReport
import printscript.reader.FileCodeReader
import printscript.usecases.CheckFormat

class CheckCommand : SourceFileCommand("check", "Check that a PrintScript file matches the formatter") {
    override fun run() {
        val kit = currentContext.findObject<VersionKit>() ?: error("Language kit not loaded")
        emit(
            presentReport(
                {
                    CheckFormat.checkFormat(
                        kit.configs.lang,
                        kit.configs.grammar,
                        FileCodeReader(file),
                        Files.readString(Path.of(file)),
                        kit.configs.formatter,
                        kit.langKit,
                    )
                },
                ::formatError,
            ),
        )
    }
}
