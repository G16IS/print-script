package printscript.statement

import printscript.InterpreterContext
import printscript.UninitializedValue
import printscript.ValueCoercion
import printscript.error.RuntimeError
import printscript.expression.ExpressionSolver
import printscript.node.AstNames
import printscript.node.namedChild
import printscript.node.tokenValue
import printscript.syntax.SyntaxNode
import printscript.util.Result
import printscript.util.flatMap
import printscript.util.map

/**
 * `let <id>: <type> (= <expr>)?;`
 *
 * El tipo declarado se guarda en el contexto porque es lo que define a qué
 * convertir un `readInput` / `readEnv` asignado a esta variable.
 */
open class DeclarationExecutor(
    override val nodeNames: Set<String>,
) : StatementExecutor {
    override fun execute(
        node: SyntaxNode,
        context: InterpreterContext,
        solver: ExpressionSolver,
        blocks: BlockExecutor,
    ): Result<InterpreterContext, RuntimeError> =
        node.namedChild(AstNames.ID).flatMap { it.tokenValue() }.flatMap { name ->
            val declaredType =
                node
                    .childOrNull(AstNames.TYPE)
                    ?.token
                    ?.value
                    ?.orElse(null)
            val expression = node.findOrNull(AstNames.EXPRESSION)
            if (expression == null) {
                Result.Ok(context.declareVariable(name, UninitializedValue, declaredType))
            } else {
                solver.solve(expression, context).flatMap { result ->
                    ValueCoercion.toDeclared(result.value, declaredType, expression.location).map { value ->
                        context.declareVariable(name, value, declaredType)
                    }
                }
            }
        }
}
