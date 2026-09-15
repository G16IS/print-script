package printscript.statement

import printscript.BooleanValue
import printscript.InterpreterContext
import printscript.error.InvalidCondition
import printscript.error.RuntimeError
import printscript.expression.ExpressionSolver
import printscript.node.AstNames
import printscript.node.namedChild
import printscript.syntax.SyntaxNode
import printscript.util.Result
import printscript.util.flatMap
import printscript.util.map

/**
 * `if (<expr boolean>) { … } (else { … })?`
 *
 * Cada rama corre en un scope hijo que se descarta al salir, así que lo que se
 * declara adentro del bloque no sobrevive. Las asignaciones a variables de
 * afuera sí, porque [InterpreterContext.assignVariable] reconstruye la cadena.
 */
object IfExecutor : StatementExecutor {
    override val nodeNames = setOf(AstNames.IF)

    override fun execute(
        node: SyntaxNode,
        context: InterpreterContext,
        solver: ExpressionSolver,
        blocks: BlockExecutor,
    ): Result<InterpreterContext, RuntimeError> =
        node.namedChild(AstNames.EXPRESSION).flatMap { condition ->
            solver.solve(condition, context).flatMap { solved ->
                val taken = solved.value
                if (taken !is BooleanValue) {
                    return@flatMap Result.Err(InvalidCondition(condition.location))
                }
                val branch = if (taken.value) thenBlock(node) else elseBlock(node)
                runBranch(branch, context, blocks)
            }
        }

    private fun thenBlock(node: SyntaxNode): SyntaxNode? = node.childOrNull(AstNames.BLOCK)

    /** `else-clause` es un `optional`: envuelve 0 o 1 `else-block`. */
    private fun elseBlock(node: SyntaxNode): SyntaxNode? =
        node
            .childOrNull(AstNames.ELSE_CLAUSE)
            ?.children
            ?.singleOrNull()
            ?.childOrNull(AstNames.BLOCK)

    private fun runBranch(
        block: SyntaxNode?,
        context: InterpreterContext,
        blocks: BlockExecutor,
    ): Result<InterpreterContext, RuntimeError> {
        if (block == null) return Result.Ok(context)
        val statements = block.childOrNull(AstNames.STATEMENTS)?.children ?: emptyList()
        return blocks.execute(statements, context.childScope()).map { context }
    }
}
