package printscript

import printscript.expression.DefaultExpressionSolver
import printscript.expression.ExpressionEvaluator
import printscript.expression.GroupEvaluator
import printscript.expression.binaryoperation.BinaryOperationEvaluator
import printscript.expression.binaryoperation.DefaultTypeConfiguration
import printscript.expression.binaryoperation.TypeConfiguration
import printscript.expression.call.CallEvaluator
import printscript.expression.literal.IdentifierEvaluator
import printscript.expression.literal.NumberLiteralEvaluator
import printscript.expression.literal.StringLiteralEvaluator
import printscript.statement.ExpressionStatementExecutor
import printscript.statement.StatementExecutor
import printscript.statement.VariableDeclarationExecutor

object DefaultInterpreterFactory {
    fun create(typeConfiguration: TypeConfiguration = DefaultTypeConfiguration): DefaultInterpreter {
        val expressionSolver = DefaultExpressionSolver(defaultEvaluators(typeConfiguration))

        return DefaultInterpreter(expressionSolver, defaultStatementExecutors())
    }

    internal fun defaultEvaluators(
        typeConfiguration: TypeConfiguration = DefaultTypeConfiguration,
    ): List<ExpressionEvaluator> =
        listOf(
            NumberLiteralEvaluator,
            StringLiteralEvaluator,
            IdentifierEvaluator,
            GroupEvaluator,
            BinaryOperationEvaluator(typeConfiguration),
            CallEvaluator(),
        )

    internal fun defaultStatementExecutors(): List<StatementExecutor> =
        listOf(
            VariableDeclarationExecutor,
            ExpressionStatementExecutor,
        )
}
