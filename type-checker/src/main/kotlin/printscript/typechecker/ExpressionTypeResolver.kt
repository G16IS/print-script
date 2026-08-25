package printscript.typechecker

import printscript.domain.TypeSystemConfig
import printscript.syntax.SyntaxNode
import printscript.typechecker.handlers.BinaryOrPrimaryHandler
import printscript.typechecker.handlers.CallHandler
import printscript.typechecker.handlers.ExpressionKindHandler
import printscript.typechecker.handlers.GroupHandler
import printscript.typechecker.handlers.IdentifierHandler
import printscript.typechecker.handlers.LiteralHandler
import printscript.typechecker.handlers.PrimaryHandler
import printscript.util.Result

interface ExpressionTypeResolver {
    fun resolve(
        expression: SyntaxNode,
        scope: ScopeStack,
        config: TypeSystemConfig,
    ): Result<String, TypeError>
}

class DefaultExpressionTypeResolver(
    kindHandlers: List<ExpressionKindHandler>? = null,
) : ExpressionTypeResolver {
    private val handlers: Map<String, ExpressionKindHandler> by lazy {
        (kindHandlers ?: builtInHandlers()).associateBy { it.kind }
    }

    private fun builtInHandlers(): List<ExpressionKindHandler> =
        listOf(
            LiteralHandler(),
            IdentifierHandler(),
            BinaryOrPrimaryHandler(this),
            GroupHandler(this),
            CallHandler(this),
            PrimaryHandler(this),
        )

    override fun resolve(
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
