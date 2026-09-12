package printscript.typechecker

import printscript.typechecker.handlers.ExpressionKindHandler

interface ExpressionKindHandlerFactory {
    fun create(expressionTypeResolver: ExpressionTypeResolver): List<ExpressionKindHandler>
}
