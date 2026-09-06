package printscript.cli.command

import printscript.SideEffect
import printscript.cli.CommandEffects
import printscript.cli.SourceFileCommand
import printscript.cli.SourceFiles
import printscript.cli.emit
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.TypeSystemConfig
import printscript.util.Result
import usecases.ExecuteCode
import usecases.ExecutionFailure

class RunCommand(
    private val lang: LanguageConfig,
    private val grammar: Grammar,
    private val typeSystem: TypeSystemConfig,
    private val sources: SourceFiles,
    private val effects: CommandEffects<Result<List<SideEffect>, ExecutionFailure>>,
) : SourceFileCommand("run", "Execute a PrintScript file", printOk = false) {
    override fun run() {
        emit(
            effects.handle {
                ExecuteCode.execute(lang, grammar, typeSystem, sources.reader(file))
            },
            printOk = false,
        )
    }
}
