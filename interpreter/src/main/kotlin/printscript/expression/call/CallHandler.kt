package printscript.expression.call

import printscript.SideEffectManager
import printscript.error.RuntimeError
import printscript.expression.EvalResult
import printscript.syntax.SyntaxNode
import printscript.util.Result

interface CallHandler {
    val callee: String

    /**
     * [effects] permite disparar un efecto y quedarse con lo que devuelve, que
     * es lo que necesita `readInput` / `readEnv`. Un handler que sólo emite
     * (como `println`) puede ignorarlo y sumar el efecto a [EvalResult
     * .sideEffects]: de eso se encarga [CallEvaluator].
     */
    fun handle(
        result: EvalResult,
        node: SyntaxNode,
        effects: SideEffectManager,
    ): Result<EvalResult, RuntimeError>
}
