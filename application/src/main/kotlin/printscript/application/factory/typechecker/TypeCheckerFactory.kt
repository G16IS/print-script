package printscript.application.factory.typechecker

import printscript.domain.TypeSystemConfig
import printscript.error.LanguageVersionNotFound
import printscript.error.RuntimeError
import printscript.typechecker.DefaultTypeCheckerFactory
import printscript.typechecker.ExpressionKindHandlerFactory
import printscript.typechecker.TypeChecker
import printscript.util.Result
import printscript.util.flatMap

object TypeCheckerFactory {
    fun create(
        config: TypeSystemConfig,
        version: String,
    ): Result<TypeChecker, RuntimeError> {
        return getKindHandlerFactory(version).flatMap { factory ->
            return Result.Ok(
                DefaultTypeCheckerFactory.create(
                    config,
                    factory,
                ),
            )
        }
    }

    private fun getKindHandlerFactory(version: String): Result<ExpressionKindHandlerFactory, RuntimeError> =
        when (version) {
            "1" -> Result.Ok(V1KindHandlerFactory())
            else -> Result.Err(LanguageVersionNotFound(version))
        }
}
