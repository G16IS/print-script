package printscript.cli.command

import printscript.cli.SourceFileCommand
import printscript.cli.VersionKit
import printscript.cli.emit
import printscript.cli.formatError
import printscript.cli.presentReport
import printscript.reader.FileCodeReader
import printscript.usecases.LintProgram

class LintCommand : SourceFileCommand("lint", "Lint a PrintScript file") {
    override fun run() {
        val kit = currentContext.findObject<VersionKit>() ?: error("Language kit not loaded")
        emit(
            presentReport(
                {
                    LintProgram.lint(
                        kit.configs.lang,
                        kit.configs.grammar,
                        FileCodeReader(file),
                        kit.configs.linterConfig,
                        kit.langKit,
                    )
                },
                ::formatError,
            ),
        )
    }
}
