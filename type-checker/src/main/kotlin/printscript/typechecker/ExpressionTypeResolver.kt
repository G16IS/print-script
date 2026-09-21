package printscript.typechecker

import printscript.domain.TypeSystemConfig
import printscript.error.TypeError
import printscript.syntax.SyntaxNode
import printscript.typechecker.handlers.ExpressionKindHandler
import printscript.util.Result

class ExpressionTypeResolver(
    kindHandlerFactory: ExpressionKindHandlerFactory,
) {
    private val handlers: Map<String, ExpressionKindHandler> by lazy {
        kindHandlerFactory.create(this).associateBy { it.kind }
    }

    fun resolve(
        expression: SyntaxNode,
        scope: ScopeStack,
        config: TypeSystemConfig,
    ): Result<String, TypeError> {
        val nodeConfig =
            config.nodes[expression.name] ?: return Result.Err(
                TypeError("Nodo no reconocido: '${expression.name}'", expression.location),
            )

        val handler = handlers[nodeConfig.kind]

        return handler?.resolve(expression, scope, config)
            ?: Result.Err(
                TypeError(
                    "Kind '${nodeConfig.kind}' no soportado para resolución de tipo",
                    expression.location,
                ),
            )
    }
}
