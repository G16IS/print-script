package printscript.factory.typechecker

import printscript.typechecker.ExpressionKindHandlerFactory
import printscript.typechecker.ExpressionTypeResolver
import printscript.typechecker.handlers.BinaryOrPrimaryHandler
import printscript.typechecker.handlers.CallHandler
import printscript.typechecker.handlers.ExpressionKindHandler
import printscript.typechecker.handlers.GroupHandler
import printscript.typechecker.handlers.IdentifierHandler
import printscript.typechecker.handlers.LiteralHandler
import printscript.typechecker.handlers.PrimaryHandler

class V1KindHandlerFactory : ExpressionKindHandlerFactory {
    override fun create(expressionTypeResolver: ExpressionTypeResolver): List<ExpressionKindHandler> =
        listOf(
            LiteralHandler(),
            IdentifierHandler(),
            BinaryOrPrimaryHandler(expressionTypeResolver),
            GroupHandler(expressionTypeResolver),
            CallHandler(expressionTypeResolver),
            PrimaryHandler(expressionTypeResolver),
        )
}
