package printscript.statement

import printscript.InterpreterContext
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
 * Reassigns an already declared variable. Mutability and type compatibility are the
 * type-checker's job: the interpreter trusts a validated program.
 */
object AssignmentExecutor : StatementExecutor {
    override val nodeNames = setOf(AstNames.ASSIGNMENT)

    override fun execute(
        node: SyntaxNode,
        context: InterpreterContext,
        solver: ExpressionSolver,
    ): Result<InterpreterContext, RuntimeError> =
        node.namedChild(AstNames.ID).flatMap { it.tokenValue() }.flatMap { name ->
            node.namedChild(AstNames.EXPRESSION).flatMap { expression ->
                solver.solve(expression, context).flatMap { solved ->
                    context.assignVariable(name, solved.value, node.location)
                }
            }
        }
}
