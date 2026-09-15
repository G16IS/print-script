package printscript

import printscript.expression.ExpressionEvaluator
import printscript.expression.GroupEvaluator
import printscript.expression.binaryoperation.BinaryOperationEvaluator
import printscript.expression.binaryoperation.DefaultTypeConfiguration
import printscript.expression.binaryoperation.TypeConfiguration
import printscript.expression.call.CallEvaluator
import printscript.expression.literal.IdentifierEvaluator
import printscript.expression.literal.NumberLiteralEvaluator
import printscript.expression.literal.StringLiteralEvaluator
import printscript.statement.AssignmentExecutor
import printscript.statement.ExpressionStatementExecutor
import printscript.statement.StatementExecutor
import printscript.statement.VariableDeclarationExecutor

object DefaultInterpreterComponents {
    val typeConfiguration: TypeConfiguration = DefaultTypeConfiguration

    fun evaluators(sideEffectManager: SideEffectManager): List<ExpressionEvaluator> =
        listOf(
            NumberLiteralEvaluator,
            StringLiteralEvaluator,
            IdentifierEvaluator,
            GroupEvaluator,
            BinaryOperationEvaluator(typeConfiguration),
            CallEvaluator(sideEffectManager = sideEffectManager),
        )

    val executors: List<StatementExecutor> =
        listOf(
            VariableDeclarationExecutor,
            AssignmentExecutor,
            ExpressionStatementExecutor,
        )
}
