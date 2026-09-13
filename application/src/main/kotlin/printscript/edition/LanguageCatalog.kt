package printscript.edition

import printscript.DefaultInterpreterComponents
import printscript.error.LanguageVersionNotFound
import printscript.error.RuntimeError
import printscript.parse.RuleHandlers
import printscript.typechecker.DefaultKindHandlerFactory
import printscript.util.Result
import printscript.util.flatMap

object LanguageCatalog {
    val v10 =
        LanguageKit(
            version = LanguageVersion(1, 0),
            resourceSuffix = "1.0",
            parserHandlers = RuleHandlers.defaults(),
            kindHandlerFactory = DefaultKindHandlerFactory(),
            typeConfiguration = DefaultInterpreterComponents.typeConfiguration,
            evaluators = DefaultInterpreterComponents.evaluators,
            executors = DefaultInterpreterComponents.executors,
        )

    val v11 =
        v10.copy(
            version = LanguageVersion(1, 1),
            resourceSuffix = "1.1",
        )

    fun of(raw: String): Result<LanguageKit, RuntimeError> =
        LanguageVersion
            .parse(raw)
            .flatMap { version ->
                val kit =
                    kits[version] ?: return@flatMap Result
                        .Err(LanguageVersionNotFound(raw))

                return@flatMap Result.Ok(kit)
            }

    private val kits =
        mapOf(
            LanguageVersion(1, 0) to v10,
            LanguageVersion(1, 1) to v11,
        )
}
