package printscript.edition

import printscript.DefaultInterpreterComponents
import printscript.SideEffectManager
import printscript.error.LanguageVersionNotFound
import printscript.error.RuntimeError
import printscript.parser.parse.RuleHandlers
import printscript.typechecker.DefaultKindHandlerFactory
import printscript.util.Result
import printscript.util.flatMap

object LanguageCatalog {
    fun v10(sideEffectManager: SideEffectManager) =
        LanguageKit(
            version = LanguageVersion(1, 0),
            resourceSuffix = "1.0",
            parserHandlers = RuleHandlers.defaults(),
            kindHandlerFactory = DefaultKindHandlerFactory(),
            typeConfiguration = DefaultInterpreterComponents.typeConfiguration,
            evaluators =
                DefaultInterpreterComponents.evaluators(
                    sideEffectManager = sideEffectManager,
                ),
            executors = DefaultInterpreterComponents.executors,
            sideEffectManager = sideEffectManager,
        )

    fun v11(sideEffectManager: SideEffectManager) =
        v10(sideEffectManager).copy(
            version = LanguageVersion(1, 1),
            resourceSuffix = "1.1",
            evaluators =
                DefaultInterpreterComponents.evaluatorsV11(
                    sideEffectManager = sideEffectManager,
                ),
            executors = DefaultInterpreterComponents.executorsV11,
        )

    fun of(
        raw: String,
        sideEffectManager: SideEffectManager,
    ): Result<LanguageKit, RuntimeError> =
        LanguageVersion
            .parse(raw)
            .flatMap { version ->
                val kit =
                    kits(sideEffectManager)[version] ?: return@flatMap Result
                        .Err(LanguageVersionNotFound(raw))

                return@flatMap Result.Ok(kit)
            }

    private fun kits(sideEffectManager: SideEffectManager) =
        mapOf(
            LanguageVersion(1, 0) to v10(sideEffectManager),
            LanguageVersion(1, 1) to v11(sideEffectManager),
        )
}
