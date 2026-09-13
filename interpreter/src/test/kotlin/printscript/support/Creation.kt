package printscript.support

import printscript.Interpreter
import printscript.expression.ExpressionEvaluator
import printscript.expression.GroupEvaluator
import printscript.expression.binaryoperation.BinaryOperationEvaluator
import printscript.expression.binaryoperation.DefaultTypeConfiguration
import printscript.expression.call.CallEvaluator
import printscript.expression.literal.IdentifierEvaluator
import printscript.expression.literal.NumberLiteralEvaluator
import printscript.expression.literal.StringLiteralEvaluator
import printscript.factory.InterpreterFactory
import printscript.statement.ExpressionStatementExecutor
import printscript.statement.StatementExecutor
import printscript.statement.VariableDeclarationExecutor
import printscript.util.Result

fun mockv1Evaluators(): List<ExpressionEvaluator> =
    listOf(
        NumberLiteralEvaluator,
        StringLiteralEvaluator,
        IdentifierEvaluator,
        GroupEvaluator,
        BinaryOperationEvaluator(DefaultTypeConfiguration),
        CallEvaluator(),
    )

fun mockv1Executors(): List<StatementExecutor> =
    listOf(
        VariableDeclarationExecutor,
        ExpressionStatementExecutor,
    )

fun createInterpreter(version: String): Interpreter = (InterpreterFactory.create(version) as Result.Ok).value
