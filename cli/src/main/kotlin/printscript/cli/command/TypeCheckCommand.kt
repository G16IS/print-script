package printscript.cli.command

import printscript.cli.SourceFileCommand
import printscript.cli.emit
import printscript.cli.formatTypeError
import printscript.cli.presentReport
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.TypeSystemConfig
import printscript.infrastructure.reader.FileCodeReader
import usecases.TypecheckCode

class TypeCheckCommand(
    private val lang: LanguageConfig,
    private val grammar: Grammar,
    private val typeSystem: TypeSystemConfig,
) : SourceFileCommand("typecheck", "Type-check a PrintScript file") {
    override fun run() {
        emit(
            presentReport(
                { TypecheckCode.typecheck(lang, grammar, typeSystem, FileCodeReader(file)) },
                ::formatTypeError,
            ),
        )
    }
}
