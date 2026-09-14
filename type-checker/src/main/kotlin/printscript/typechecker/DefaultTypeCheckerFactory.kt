package printscript.typechecker

import printscript.domain.TypeSystemConfig

object DefaultTypeCheckerFactory {
    fun create(
        config: TypeSystemConfig,
        kindHandlerFactory: ExpressionKindHandlerFactory,
    ): TypeChecker =
        DefaultTypeChecker(
            config,
            DefaultExpressionTypeResolver(kindHandlerFactory),
        )
}
