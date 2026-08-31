package printscript.formatter.config

import printscript.domain.FormatRuleSpec
import printscript.domain.FormatterLanguageConfig
import printscript.domain.FormatterRulesConfig
import printscript.domain.LanguageFormatRuleSpec
import printscript.domain.UserRuleBinding
import printscript.formatter.FormatError
import printscript.formatter.UnknownRuleType
import printscript.formatter.factories.FormatRuleFactory
import printscript.formatter.factories.ResolvedFormatRule
import printscript.formatter.rules.FormatRule
import printscript.util.Result
import printscript.util.flatMap
import printscript.util.map

class FormatRuleLoader(
    private val factories: List<FormatRuleFactory>,
) {
    private val byType =
        factories
            .flatMap { factory -> factory.types.map { type -> type to factory } }
            .toMap()

    fun load(
        language: FormatterLanguageConfig,
        user: FormatterRulesConfig = FormatterRulesConfig(),
    ): Result<List<FormatRule>, FormatError> =
        instantiateAll(language.rules.map { it.toResolved() })
            .flatMap { languageRules ->
                instantiateUser(language.userBindings, user.rules)
                    .map { userRules -> languageRules + userRules }
            }

    private fun instantiateUser(
        bindings: List<UserRuleBinding>,
        userRules: List<FormatRuleSpec>,
    ): Result<List<FormatRule>, FormatError> {
        val bindingByUserType = bindings.associateBy { it.userType }
        val resolved =
            mergeUserRules(userRules, bindingByUserType.keys).map { spec ->
                val binding =
                    bindingByUserType[spec.type]
                        ?: return Result.Err(UnknownRuleType(spec.type))
                binding.toResolved(spec)
            }

        return instantiateAll(resolved)
    }

    private fun mergeUserRules(
        userRules: List<FormatRuleSpec>,
        knownUserTypes: Set<String>,
    ): List<FormatRuleSpec> {
        val byType = userRules.associateBy { it.type }
        val withDefaults =
            USER_DEFAULTS
                .filter { it.type in knownUserTypes }
                .map { default -> byType[default.type] ?: default }
        val extra =
            userRules.filter { spec ->
                USER_DEFAULTS.none { it.type == spec.type }
            }

        return withDefaults + extra
    }

    private fun instantiateAll(specs: List<ResolvedFormatRule>): Result<List<FormatRule>, FormatError> {
        val start: Result<List<FormatRule>, FormatError> = Result.Ok(emptyList())

        return specs.fold(start) { acc, spec ->
            acc.flatMap { rules ->
                instantiateOne(spec).map { rules + it }
            }
        }
    }

    private fun instantiateOne(spec: ResolvedFormatRule): Result<FormatRule, FormatError> {
        val factory =
            byType[spec.type]
                ?: return Result.Err(UnknownRuleType(spec.type))

        return factory.create(spec)
    }

    companion object {
        val USER_DEFAULTS =
            listOf(
                FormatRuleSpec(type = "space-before-colon", enabled = true),
                FormatRuleSpec(type = "space-after-colon", enabled = true),
                FormatRuleSpec(type = "space-around-assign", enabled = true),
                FormatRuleSpec(type = "newlines-before-println", count = 1),
            )
    }
}

private fun LanguageFormatRuleSpec.toResolved() =
    ResolvedFormatRule(
        type = type,
        token = token,
        value = value,
        previous = previous,
        enabled = true,
        count = 1,
    )

private fun UserRuleBinding.toResolved(spec: FormatRuleSpec) =
    ResolvedFormatRule(
        type = type,
        token = token,
        value = value,
        previous = previous,
        enabled = spec.enabled ?: defaultEnabled,
        count = spec.count ?: defaultCount,
    )
