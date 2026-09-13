package printscript.factory

import printscript.DefaultInterpreterFactory
import printscript.Interpreter
import printscript.error.LanguageVersionNotFound
import printscript.error.RuntimeError
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
import printscript.util.Result
import printscript.util.flatMap

object InterpreterFactory {
    fun create(version: String): Result<Interpreter, RuntimeError> {
        return getTypeConfiguration(version).flatMap { configuration ->
            getEvaluators(
                version,
                configuration,
            ).flatMap { evaluators ->
                getStatementExecutors(version).flatMap { executors ->
                    return Result.Ok(DefaultInterpreterFactory.create(evaluators, executors))
                }
            }
        }
    }

    private fun getTypeConfiguration(version: String): Result<TypeConfiguration, RuntimeError> =
        when (version) {
            "1" -> Result.Ok(DefaultTypeConfiguration)
            else -> Result.Err(LanguageVersionNotFound(version))
        }

    private fun getEvaluators(
        version: String,
        typeConfig: TypeConfiguration,
    ): Result<List<ExpressionEvaluator>, RuntimeError> =
        when (version) {
            "1" -> Result.Ok(getV1Evaluators(typeConfig))
            else -> Result.Err(LanguageVersionNotFound(version))
        }

    private fun getV1Evaluators(typeConfig: TypeConfiguration): List<ExpressionEvaluator> =
        listOf(
            NumberLiteralEvaluator,
            StringLiteralEvaluator,
            IdentifierEvaluator,
            GroupEvaluator,
            BinaryOperationEvaluator(typeConfig),
            CallEvaluator(),
        )

    private fun getStatementExecutors(version: String): Result<List<StatementExecutor>, RuntimeError> =
        when (version) {
            "1" -> Result.Ok(getV1StatementExecutors())
            else -> Result.Err(LanguageVersionNotFound(version))
        }

    private fun getV1StatementExecutors(): List<StatementExecutor> =
        listOf(
            VariableDeclarationExecutor,
            ExpressionStatementExecutor,
        )
}
