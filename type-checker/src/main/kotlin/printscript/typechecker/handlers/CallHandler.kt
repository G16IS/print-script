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
        val argNames = config.nodes[node.name]?.args ?: emptyList()

        val args =
            argNames.flatMap { name ->
                node.children.filter { it.name == name }
            }

        val failed =
            args
                .asSequence()
                .map { resolver.resolve(it, scope, config) }
                .firstOrNull { !it.isOk }

        return failed ?: Result.Ok("")
    }
}
