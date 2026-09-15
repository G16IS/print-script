package printscript

import printscript.expression.ExpressionEvaluator
import printscript.expression.GroupEvaluator
import printscript.expression.binaryoperation.BinaryOperationEvaluator
import printscript.expression.binaryoperation.DefaultTypeConfiguration
import printscript.expression.binaryoperation.TypeConfiguration
import printscript.expression.call.CallEvaluator
import printscript.expression.call.PrintlnHandler
import printscript.expression.call.ReadEnvCallHandler
import printscript.expression.call.ReadInputCallHandler
import printscript.expression.literal.BooleanLiteralEvaluator
import printscript.expression.literal.IdentifierEvaluator
import printscript.expression.literal.NumberLiteralEvaluator
import printscript.expression.literal.StringLiteralEvaluator
import printscript.statement.AssignmentExecutor
import printscript.statement.ConstantDeclarationExecutor
import printscript.statement.ExpressionStatementExecutor
import printscript.statement.IfExecutor
import printscript.statement.StatementExecutor
import printscript.statement.VariableDeclarationExecutor

/**
 * Piezas del intérprete por versión del lenguaje.
 *
 * v1.0 y v1.1 se listan por separado a propósito: correr un programa 1.1 con
 * `--version 1.0` tiene que fallar, no ejecutarse igual.
 */
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

    /** v1.1 suma literales boolean y las llamadas de lectura. */
    fun evaluatorsV11(sideEffectManager: SideEffectManager): List<ExpressionEvaluator> =
        listOf(
            NumberLiteralEvaluator,
            StringLiteralEvaluator,
            BooleanLiteralEvaluator,
            IdentifierEvaluator,
            GroupEvaluator,
            BinaryOperationEvaluator(typeConfiguration),
            CallEvaluator(
                handlers = listOf(PrintlnHandler, ReadInputCallHandler, ReadEnvCallHandler),
                sideEffectManager = sideEffectManager,
            ),
        )

    /** v1.1 suma `const` y el `if`. */
    val executorsV11: List<StatementExecutor> = executors + ConstantDeclarationExecutor + IfExecutor
}
