package printscript.formatter.config

import printscript.domain.FormatRuleSpec
import printscript.domain.FormatterRulesConfig
import printscript.formatter.FormatError
import printscript.formatter.UnknownRuleType
import printscript.formatter.UserDeclaredFixedRule
import printscript.formatter.factories.FormatRuleFactory
import printscript.formatter.rules.FormatRule
import printscript.util.Result
import printscript.util.flatMap
import printscript.util.map

class FormatRuleLoader(
    private val factories: List<FormatRuleFactory>,
) {
    private val byType = factories.associateBy { it.type }

    fun load(
        language: FormatterRulesConfig,
        user: FormatterRulesConfig = FormatterRulesConfig(),
    ): Result<List<FormatRule>, FormatError> =
        instantiate(language.rules, fromUser = false)
            .flatMap { languageRules ->
                instantiate(mergeUserRules(user.rules), fromUser = true)
                    .map { userRules -> languageRules + userRules }
            }

    private fun mergeUserRules(userRules: List<FormatRuleSpec>): List<FormatRuleSpec> {
        val byType = userRules.associateBy { it.type }
        val withDefaults =
            USER_DEFAULTS.map { default ->
                byType[default.type] ?: default
            }
        val extra =
            userRules.filter { spec ->
                USER_DEFAULTS.none { it.type == spec.type }
            }

        return withDefaults + extra
    }

    private fun instantiate(
        specs: List<FormatRuleSpec>,
        fromUser: Boolean,
    ): Result<List<FormatRule>, FormatError> {
        val start: Result<List<FormatRule>, FormatError> = Result.Ok(emptyList())

        return specs
            .fold(start) { acc, spec ->
                acc.flatMap { rules ->
                    instantiateOne(spec, fromUser)
                        .map { rules + it }
                }
            }
    }

    private fun instantiateOne(
        spec: FormatRuleSpec,
        fromUser: Boolean,
    ): Result<FormatRule, FormatError> {
        val factory =
            byType[spec.type]
                ?: return Result.Err(UnknownRuleType(spec.type))

        return if (fromUser && !factory.userConfigurable) {
            Result.Err(UserDeclaredFixedRule(spec.type))
        } else {
            factory.create(spec.params())
        }
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
