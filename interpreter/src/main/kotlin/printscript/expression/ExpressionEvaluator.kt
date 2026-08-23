package printscript.expression

import printscript.InterpreterContext
import printscript.RuntimeValue
import printscript.syntax.SyntaxNode

interface ExpressionEvaluator {

    fun canHandle(node: SyntaxNode): Boolean
    fun evaluate(node: SyntaxNode, context: InterpreterContext, solver: ExpressionSolver): RuntimeValue
}
