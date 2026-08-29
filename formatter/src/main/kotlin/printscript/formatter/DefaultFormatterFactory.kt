package printscript.formatter

import printscript.domain.FormatterRulesConfig
import printscript.domain.Grammar
import printscript.domain.TokenLexemes
import printscript.formatter.config.FormatRuleLoader
import printscript.formatter.factories.FormatRuleFactories
import printscript.formatter.rules.FormatRule
import printscript.util.Result
import printscript.util.map

object DefaultFormatterFactory {
    fun create(
        rules: List<FormatRule>,
        grammar: Grammar,
        lexemes: TokenLexemes,
    ): Formatter =
        DefaultFormatter(
            DefaultRuleRegistry(rules),
            GrammarWalker(grammar, lexemes),
        )

    fun createFromConfig(
        language: FormatterRulesConfig,
        user: FormatterRulesConfig = FormatterRulesConfig(),
        grammar: Grammar,
        lexemes: TokenLexemes,
    ): Result<Formatter, FormatError> =
        FormatRuleLoader(FormatRuleFactories.defaults())
            .load(language, user)
            .map { create(it, grammar, lexemes) }
}
