package printscript.expression

import printscript.InterpreterContext
import printscript.RuntimeValue
import printscript.syntax.SyntaxNode

class ExpressionSolver(private val evaluators: List<ExpressionEvaluator>) {
    fun solve(node: SyntaxNode, context: InterpreterContext): RuntimeValue {
        val evaluator = evaluators.firstOrNull { it.canHandle(node) }
            ?: throw InterpreterException("No hay evaluator para nodo '${node.type}'")
        return evaluator.evaluate(node, context, this)
    }
}
