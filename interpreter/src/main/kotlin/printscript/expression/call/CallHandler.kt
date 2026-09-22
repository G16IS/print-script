package printscript.expression.call

import printscript.definitions.SideEffectManager
import printscript.error.RuntimeError
import printscript.expression.EvalResult
import printscript.syntax.SyntaxNode
import printscript.util.Result

interface CallHandler {
    val callee: String

    /**
     * Cada handler dispara su efecto con [effects.handle] en el momento.
     * `readInput` / `readEnv` usan el String que devuelve; `println` no.
     */
    fun handle(
        result: EvalResult,
        node: SyntaxNode,
        effects: SideEffectManager,
    ): Result<EvalResult, RuntimeError>
}
