package usecases

import printscript.domain.FormatterLanguageConfig
import printscript.domain.FormatterRulesConfig
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.TokenLexemes
import printscript.error.FormatError
import printscript.formatter.DefaultFormatterFactory
import printscript.formatter.Formatter
import printscript.util.Result

object LoadFormatter {
    fun load(
        grammar: Grammar,
        langConfig: LanguageConfig,
        language: FormatterLanguageConfig,
        user: FormatterRulesConfig,
        defaults: FormatterRulesConfig,
    ): Result<Formatter, FormatError> {
        val lexemes = TokenLexemes.from(langConfig)

        return DefaultFormatterFactory
            .createFromConfig(language, user, defaults, grammar, lexemes)
    }
}
