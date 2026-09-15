package printscript.statement

import printscript.InterpreterContext
import printscript.ValueCoercion
import printscript.error.RuntimeError
import printscript.expression.ExpressionSolver
import printscript.node.AstNames
import printscript.node.namedChild
import printscript.node.tokenValue
import printscript.syntax.SyntaxNode
import printscript.util.Result
import printscript.util.flatMap

/**
 * `<id> = <expr>;`
 *
 * Mutability and type compatibility are the type-checker's job: the interpreter
 * trusts a validated program. Lo único que resuelve acá es a qué tipo convertir
 * un `readInput` / `readEnv`, que sale del tipo con el que se declaró la variable.
 */
object AssignmentExecutor : StatementExecutor {
    override val nodeNames = setOf(AstNames.ASSIGNMENT)

    override fun execute(
        node: SyntaxNode,
        context: InterpreterContext,
        solver: ExpressionSolver,
        blocks: BlockExecutor,
    ): Result<InterpreterContext, RuntimeError> =
        node.namedChild(AstNames.ID).flatMap { it.tokenValue() }.flatMap { name ->
            node.namedChild(AstNames.EXPRESSION).flatMap { expression ->
                solver.solve(expression, context).flatMap { solved ->
                    ValueCoercion
                        .toDeclared(solved.value, context.typeOf(name), expression.location)
                        .flatMap { value -> context.assignVariable(name, value, node.location) }
                }
            }
        }
}
