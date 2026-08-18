package printscript

import printscript.error.ParseErrors
import printscript.parse.RuleEvaluator
import printscript.domain.Grammar
import printscript.syntax.SyntaxProgram
import printscript.token.LexerTokenSource
import printscript.token.TokenSource

class DefaultParser(
    private val grammar: Grammar,
    private val evaluator: RuleEvaluator
) : Parser {
    private var boundLexer: Lexer? = null
    private var source: TokenSource? = null

    override fun parseNextStatement(
        tokenStream: Lexer,
        program: SyntaxProgram
    ): SyntaxProgram {
        val tokens = bind(tokenStream)
        val node = evaluator.evaluate(grammar.start, tokens)
            ?: throw ParseErrors.unexpectedStart(tokens.peek())
        return program.withStatement(node)
    }

    private fun bind(lexer: Lexer): TokenSource {
        if (boundLexer === lexer && source != null) return source!!
        boundLexer = lexer
        source = LexerTokenSource(lexer)
        return source!!
    }
}
