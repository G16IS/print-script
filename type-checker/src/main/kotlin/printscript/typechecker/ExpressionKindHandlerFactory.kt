package printscript.typechecker

import printscript.typechecker.expression.ExpressionKindHandler

interface ExpressionKindHandlerFactory {
    fun create(expressionTypeResolver: ExpressionTypeResolver): List<ExpressionKindHandler>
}
