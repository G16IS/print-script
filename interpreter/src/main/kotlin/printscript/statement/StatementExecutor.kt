package printscript.statement

import printscript.InterpreterContext
import printscript.SideEffect
import printscript.syntax.SyntaxNode

interface StatementExecutor {
    fun canHandle(node: SyntaxNode): Boolean
    fun execute(node: SyntaxNode ,context: InterpreterContext): List<SideEffect>
}
