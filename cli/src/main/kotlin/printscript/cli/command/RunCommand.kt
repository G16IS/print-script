package printscript.cli.command

import printscript.cli.SourceFileCommand
import printscript.cli.emit
import printscript.cli.presentRun
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.TypeSystemConfig
import printscript.edition.LanguageCatalog
import printscript.reader.FileCodeReader
import printscript.usecases.ExecuteCode

class RunCommand(
    private val lang: LanguageConfig,
    private val grammar: Grammar,
    private val typeSystem: TypeSystemConfig,
) : SourceFileCommand("run", "Execute a PrintScript file") {
    override fun run() {
        emit(
            presentRun {
                ExecuteCode.execute(lang, grammar, typeSystem, FileCodeReader(file), LanguageCatalog.v10)
            },
            printOk = false,
        )
    }
}
