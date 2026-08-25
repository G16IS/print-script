package printscript

import printscript.expression.ExpressionEvaluator
import printscript.expression.ExpressionSolver
import printscript.expression.GroupEvaluator
import printscript.expression.binaryoperation.BinaryOperationEvaluator
import printscript.expression.binaryoperation.DefaultTypeConfiguration
import printscript.expression.binaryoperation.TypeConfiguration
import printscript.expression.call.CallEvaluator
import printscript.expression.literal.IdentifierEvaluator
import printscript.expression.literal.NumberLiteralEvaluator
import printscript.expression.literal.StringLiteralEvaluator
import printscript.node.NodeKind
import printscript.node.NodeKindResolver
import printscript.node.PrintScriptMapping
import printscript.statement.ExpressionStatementExecutor
import printscript.statement.StatementExecutor
import printscript.statement.VariableDeclarationExecutor

object DefaultInterpreterFactory {
    fun create(
        mapping: Map<String, NodeKind> = PrintScriptMapping.mapping,
        typeConfiguration: TypeConfiguration = DefaultTypeConfiguration(),
    ): DefaultInterpreter {
        val nodeKindResolver = NodeKindResolver(mapping)
        val expressionSolver =
            ExpressionSolver(nodeKindResolver, evaluators(typeConfiguration))
        return DefaultInterpreter(nodeKindResolver, expressionSolver, statementExecutors())
    }

    private fun evaluators(typeConfiguration: TypeConfiguration): List<ExpressionEvaluator> =
        listOf(
            NumberLiteralEvaluator(),
            StringLiteralEvaluator(),
            IdentifierEvaluator(),
            GroupEvaluator(),
            BinaryOperationEvaluator(typeConfiguration),
            CallEvaluator(),
        )

    private fun statementExecutors(): List<StatementExecutor> =
        listOf(
            VariableDeclarationExecutor(),
            ExpressionStatementExecutor(),
        )
}
