package printscript.typechecker

import printscript.typechecker.handlers.BinaryOrPrimaryHandler
import printscript.typechecker.handlers.CallHandler
import printscript.typechecker.handlers.ExpressionKindHandler
import printscript.typechecker.handlers.GroupHandler
import printscript.typechecker.handlers.IdentifierHandler
import printscript.typechecker.handlers.LiteralHandler
import printscript.typechecker.handlers.PrimaryHandler

class DefaultKindHandlerFactory : ExpressionKindHandlerFactory {
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
