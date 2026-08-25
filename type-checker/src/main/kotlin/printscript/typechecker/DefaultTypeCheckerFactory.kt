package printscript.typechecker

import printscript.domain.TypeSystemConfig

object DefaultTypeCheckerFactory {
    fun create(config: TypeSystemConfig): TypeChecker =
        DefaultTypeChecker(
            config,
            DefaultExpressionTypeResolver(),
        )
}
