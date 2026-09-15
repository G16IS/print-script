package printscript.cli.command

import printscript.cli.SourceFileCommand
import printscript.cli.VersionKit
import printscript.cli.emit
import printscript.cli.presentReport
import printscript.error.formatError
import printscript.reader.FileCodeReader
import printscript.usecases.TypecheckCode

class TypeCheckCommand : SourceFileCommand("typecheck", "Type-check a PrintScript file") {
    override fun run() {
        val kit = currentContext.findObject<VersionKit>() ?: error("Language kit not loaded")
        emit(
            presentReport(
                {
                    TypecheckCode.check(
                        kit.configs.lang,
                        kit.configs.grammar,
                        kit.configs.typeSystem,
                        FileCodeReader(file),
                        kit.langKit,
                    )
                },
                ::formatError,
            ),
        )
    }
}
