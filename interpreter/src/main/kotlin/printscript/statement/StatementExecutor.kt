package printscript.statement

import printscript.InterpreterContext
import printscript.error.RuntimeError
import printscript.expression.ExpressionSolver
import printscript.syntax.SyntaxNode
import printscript.util.Result

interface StatementExecutor {
    val nodeNames: Set<String>

    /**
     * [blocks] ejecuta una lista de statements. Se inyecta igual que [solver]
     * para que un executor compuesto (`if`) pueda recursar sin conocer al
     * intérprete ni construirse en dos pasos.
     */
    fun execute(
        node: SyntaxNode,
        context: InterpreterContext,
        solver: ExpressionSolver,
        blocks: BlockExecutor,
    ): Result<InterpreterContext, RuntimeError>
}
