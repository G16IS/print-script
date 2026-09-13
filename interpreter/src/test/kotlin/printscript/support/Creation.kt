package printscript.support

import printscript.DefaultInterpreterComponents
import printscript.DefaultInterpreterFactory
import printscript.Interpreter
import printscript.edition.LanguageCatalog
import printscript.expression.ExpressionEvaluator
import printscript.statement.StatementExecutor
import printscript.util.Result
import printscript.util.map

fun defaultEvaluators(): List<ExpressionEvaluator> = DefaultInterpreterComponents.evaluators

fun defaultExecutors(): List<StatementExecutor> = DefaultInterpreterComponents.executors

fun createInterpreter(version: String): Interpreter {
    val x =
        LanguageCatalog.of(version).map { kit ->
            DefaultInterpreterFactory.create(kit.evaluators, kit.executors)
        }

    return when (x) {
        is Result.Ok -> x.value
        is Result.Err -> error("Error creating interpreter: ${x.error}")
    }
}
