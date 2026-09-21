package printscript.typechecker

import printscript.domain.TypeSystemConfig

internal object DefaultTypeCheckerFactory {
    fun create(
        config: TypeSystemConfig,
        kindHandlerFactory: ExpressionKindHandlerFactory,
    ): TypeChecker =
        DefaultTypeChecker(
            config,
            DefaultExpressionTypeResolver(kindHandlerFactory),
        )
}
