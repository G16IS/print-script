package printscript.formatter

import printscript.domain.FormatterRulesConfig
import printscript.formatter.config.FormatRuleLoader
import printscript.formatter.factories.FormatRuleFactories
import printscript.formatter.rules.FormatRule
import printscript.util.Result
import printscript.util.map

object DefaultFormatterFactory {
    fun create(rules: List<FormatRule>): Formatter = DefaultFormatter(DefaultRuleRegistry(rules))

    fun createFromConfig(
        language: FormatterRulesConfig,
        user: FormatterRulesConfig = FormatterRulesConfig(),
    ): Result<Formatter, FormatError> =
        FormatRuleLoader(FormatRuleFactories.defaults())
            .load(language, user)
            .map { create(it) }
}
