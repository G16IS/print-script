package printscript.typechecker

import printscript.typechecker.expression.BinaryOrPrimaryHandler
import printscript.typechecker.expression.CallHandler
import printscript.typechecker.expression.ExpressionKindHandler
import printscript.typechecker.expression.GroupHandler
import printscript.typechecker.expression.IdentifierHandler
import printscript.typechecker.expression.LiteralHandler
import printscript.typechecker.expression.PrimaryHandler

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
