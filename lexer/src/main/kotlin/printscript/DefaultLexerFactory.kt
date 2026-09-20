package printscript

import printscript.domain.LanguageConfig
import printscript.evaluator.ExactEvaluator
import printscript.evaluator.RegexEvaluator
import printscript.evaluator.RuleEvaluator
import printscript.reader.CodeReader

internal object DefaultLexerFactory {
    fun create(
        codeReader: CodeReader,
        langConfig: LanguageConfig,
    ): Lexer =
        TokenStream(
            codeReader,
            RuleEvaluator(
                langConfig,
                listOf(
                    RegexEvaluator(),
                    ExactEvaluator(),
                ),
            ),
            RuleDrawResolver(langConfig),
        )
}
