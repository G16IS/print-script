package printscript

import printscript.domain.LanguageConfig
import printscript.evaluator.RuleEvaluator
import printscript.reader.CodeReader

object DefaultLexerFactory {
    fun create(
        codeReader: CodeReader,
        langConfig: LanguageConfig,
    ): Lexer =
        TokenStream(
            codeReader,
            RuleEvaluator(langConfig.config),
            RuleDrawResolver(langConfig),
        )
}
