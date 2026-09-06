package printscript.cli.command

import printscript.cli.CommandEffects
import printscript.cli.SourceFileCommand
import printscript.cli.SourceFiles
import printscript.cli.emit
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.TypeSystemConfig
import printscript.syntax.SyntaxProgram
import printscript.typechecker.TypeError
import printscript.util.Report
import usecases.InterpretCode

class TypeCheckCommand(
    private val lang: LanguageConfig,
    private val grammar: Grammar,
    private val typeSystem: TypeSystemConfig,
    private val sources: SourceFiles,
    private val effects: CommandEffects<Report<SyntaxProgram, TypeError>>,
) : SourceFileCommand("typecheck", "Type-check a PrintScript file") {
    override fun run() {
        emit(
            effects.handle {
                InterpretCode.interpretCode(lang, grammar, typeSystem, sources.reader(file))
            },
        )
    }
}
