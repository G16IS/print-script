package printscript

import printscript.expression.DefaultExpressionSolver
import printscript.expression.ExpressionEvaluator
import printscript.statement.StatementExecutor

object DefaultInterpreterFactory {
    fun create(
        evaluators: List<ExpressionEvaluator>,
        statementExecutors: List<StatementExecutor>,
        sideEffectManager: SideEffectManager,
    ): DefaultInterpreter {
        val expressionSolver = DefaultExpressionSolver(evaluators)

        return DefaultInterpreter(expressionSolver, statementExecutors, sideEffectManager)
    }
}
