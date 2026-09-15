package printscript.typechecker.handlers

import printscript.domain.TypeSystemConfig
import printscript.syntax.SyntaxNode
import printscript.typechecker.ExpressionTypeResolver
import printscript.typechecker.ScopeStack
import printscript.typechecker.TypeError
import printscript.util.Result
import printscript.util.isOk

class CallHandler(
    private val resolver: ExpressionTypeResolver,
) : ExpressionKindHandler {
    override val kind: String = "call"

    override fun resolve(
        node: SyntaxNode,
        scope: ScopeStack,
        config: TypeSystemConfig,
    ): Result<String, TypeError> {
        val nodeConfig = config.nodes[node.name]
        val argNames = nodeConfig?.args ?: emptyList()

        val args =
            argNames.flatMap { name ->
                node.children.filter { it.name == name }
            }

        val failed =
            args
                .asSequence()
                .map { resolver.resolve(it, scope, config) }
                .firstOrNull { !it.isOk }

        return failed ?: Result.Ok(returnType(node, config))
    }

    /**
     * Un call no tiene tipo salvo que el config lo declare por callee
     * (`readInput` / `readEnv`). Sin entrada devuelve `""`, como antes.
     */
    private fun returnType(
        node: SyntaxNode,
        config: TypeSystemConfig,
    ): String {
        val nodeConfig = config.nodes[node.name] ?: return ""
        val calleeChild = nodeConfig.callee ?: return ""
        val callee =
            node
                .childOrNull(calleeChild)
                ?.token
                ?.value
                ?.orElse(null) ?: return ""
        return nodeConfig.returnTypes[callee] ?: ""
    }
}
