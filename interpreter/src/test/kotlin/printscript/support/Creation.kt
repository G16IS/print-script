package printscript.support

import printscript.DefaultInterpreterComponents
import printscript.DefaultInterpreterFactory
import printscript.Interpreter
import printscript.InterpreterContext
import printscript.SideEffect
import printscript.SideEffectManager
import printscript.edition.LanguageCatalog
import printscript.expression.ExpressionEvaluator
import printscript.statement.StatementExecutor
import printscript.syntax.SyntaxProgram
import printscript.util.Result
import printscript.util.map

fun defaultEvaluators(sideEffectManager: SideEffectManager = RecordingSideEffectManager()): List<ExpressionEvaluator> =
    DefaultInterpreterComponents.evaluators(sideEffectManager)

fun defaultExecutors(): List<StatementExecutor> = DefaultInterpreterComponents.executors

fun createInterpreter(
    version: String,
    sideEffectManager: SideEffectManager = RecordingSideEffectManager(),
): Interpreter {
    val x =
        LanguageCatalog.of(version, sideEffectManager).map { kit ->
            DefaultInterpreterFactory.create(kit.evaluators, kit.executors)
        }

    return when (x) {
        is Result.Ok -> x.value
        is Result.Err -> error("Error creating interpreter: ${x.error}")
    }
}

fun interpretEffects(
    version: String,
    program: SyntaxProgram,
    sideEffectManager: RecordingSideEffectManager = RecordingSideEffectManager(),
): List<SideEffect> {
    ok(createInterpreter(version, sideEffectManager).interpret(InterpreterContext(), program))
    return sideEffectManager.effects
}
