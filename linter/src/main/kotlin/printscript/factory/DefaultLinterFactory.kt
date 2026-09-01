package printscript.factory

import printscript.DefaultLinter
import printscript.Linter
import printscript.domain.LinterConfig

object DefaultLinterFactory {
    fun defaultProviders(): List<LintRuleProvider> =
        listOf(
            IdentifierFormatRuleProvider(),
            PrintlnArgumentRuleProvider(),
        )

    fun create(
        config: LinterConfig,
        providers: List<LintRuleProvider> = defaultProviders(),
    ): Linter {
        val providersById =
            providers.groupBy { it.id }.mapValues { (id, list) ->
                require(list.size == 1) { "Duplicate provider for rule id '$id'" }
                list.first()
            }

        val rules =
            config.enabled().map { (id, ruleConfig) ->
                val provider =
                    providersById[id]
                        ?: throw IllegalArgumentException("Unknown lint rule id: '$id'")
                provider.create(ruleConfig.options)
            }

        return DefaultLinter(rules)
    }
}
