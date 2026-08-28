package printscript.formatter.config

import printscript.domain.FormatRuleSpec
import printscript.domain.FormatterRulesConfig
import printscript.formatter.FormatError
import printscript.formatter.UnknownRuleType
import printscript.formatter.UserDeclaredFixedRule
import printscript.formatter.factories.FormatRuleFactory
import printscript.formatter.rules.FormatRule
import printscript.util.Result

class FormatRuleLoader(
    private val factories: List<FormatRuleFactory>,
) {
    private val byType = factories.associateBy { it.type }

    fun load(
        language: FormatterRulesConfig,
        user: FormatterRulesConfig = FormatterRulesConfig(),
    ): Result<List<FormatRule>, FormatError> =
        instantiate(language.rules, fromUser = false).let { languageRules ->
            when (languageRules) {
                is Result.Err -> languageRules
                is Result.Ok ->
                    when (val userRules = instantiate(user.rules, fromUser = true)) {
                        is Result.Err -> userRules
                        is Result.Ok -> Result.Ok(languageRules.value + userRules.value)
                    }
            }
        }

    private fun instantiate(
        specs: List<FormatRuleSpec>,
        fromUser: Boolean,
    ): Result<List<FormatRule>, FormatError> {
        val rules = mutableListOf<FormatRule>()
        for (spec in specs) {
            when (val created = instantiateOne(spec, fromUser)) {
                is Result.Err -> return created
                is Result.Ok -> rules += created.value
            }
        }
        return Result.Ok(rules)
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
}
