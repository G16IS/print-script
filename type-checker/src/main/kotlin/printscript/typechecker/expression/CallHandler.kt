package printscript.typechecker.expression

import printscript.domain.TypeSystemConfig
import printscript.error.TypeError
import printscript.syntax.SyntaxNode
import printscript.typechecker.ExpressionTypeResolver
import printscript.typechecker.ScopeStack
import printscript.util.Result
import printscript.util.isOk
import printscript.util.ok

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

        return failed ?: ok(returnType(node, config))
    }

    private fun returnType(
        node: SyntaxNode,
        config: TypeSystemConfig,
    ): String {
        val nodeConfig = config.nodes[node.name]
        val callee =
            nodeConfig
                ?.callee
                ?.let { node.childOrNull(it) }
                ?.token
                ?.value
                ?.orElse(null)
        return callee?.let { nodeConfig.returnTypes[it] } ?: NO_TYPE
    }

    private companion object {
        const val NO_TYPE = ""
    }
}
