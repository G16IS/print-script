package printscript.expression.literal

import printscript.InterpreterContext
import printscript.StringValue
import printscript.error.InvalidLiteral
import printscript.error.RuntimeError
import printscript.expression.EvalResult
import printscript.expression.ExpressionEvaluator
import printscript.expression.ExpressionSolver
import printscript.node.AstNames
import printscript.node.tokenValue
import printscript.syntax.SyntaxNode
import printscript.util.Result
import printscript.util.flatMap

object StringLiteralEvaluator : ExpressionEvaluator {
    override val nodeNames = setOf(AstNames.STRING)

    override fun evaluate(
        node: SyntaxNode,
        context: InterpreterContext,
        solver: ExpressionSolver,
    ): Result<EvalResult, RuntimeError> =
        node.tokenValue().flatMap { raw ->
            val quote = matchingQuote(raw)
            if (quote == null) {
                Result.Err(InvalidLiteral(raw, node.location))
            } else {
                Result.Ok(EvalResult.pure(StringValue(raw.substring(1, raw.lastIndex))))
            }
        }

    private fun matchingQuote(raw: String): Char? {
        val quote = raw.firstOrNull() ?: return null
        val quoted = raw.length >= 2 && raw.last() == quote
        return if (quoted && (quote == '"' || quote == '\'')) quote else null
    }
}
