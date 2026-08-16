package printscript

import printscript.ast.Location
import printscript.domain.Token
import printscript.reader.CharPosition
import printscript.reader.CodeReader
import java.util.Optional
import printscript.evaluator.MatchResult
import printscript.evaluator.MatchType
import printscript.evaluator.RuleEvaluator

class TokenStream(
    private val reader: CodeReader,
    val ruleEvaluator: RuleEvaluator,
    val ruleDrawResolver: RuleDrawResolver
) : Lexer {
    private val buffer = ArrayDeque<Token>()

    override fun nextToken(): Token = buffer.removeFirstOrNull() ?: readNextToken()
    override fun peek(offset: Int?): Token {
        val realOffset: Int = offset ?: 0

        while (buffer.size <= realOffset) buffer.addLast(readNextToken())
        return buffer[realOffset]
    }


    private fun readNextToken(): Token {
        val first = skipWhitespace()
        if (first.isEmpty) {
            val pos = reader.currentPosition()
            return Token("EOF", Optional.empty(), Location(pos, pos))
        }

        val initialPos = reader.currentPosition()
        var text = first.get().toString()
        var lastMatchResults = ruleEvaluator.evaluate(text)
        if (areAllMatchResultsInvalid(lastMatchResults)) {
            throw Error("Unexpected token at line ${initialPos.line} col ${initialPos.col}")
        }

        while (true) {
            val nextChar = reader.peek()
            if (nextChar.isEmpty) {
                return buildToken(text, lastMatchResults, initialPos, reader.currentPosition())
            }

            val matchResults = ruleEvaluator.evaluate(text + nextChar.get())
            if (areAllMatchResultsInvalid(matchResults)) {
                
                return buildToken(text, lastMatchResults, initialPos, reader.currentPosition())
            }

            reader.read()
            text += nextChar.get()
            lastMatchResults = matchResults
        }
    }

    private fun buildToken(
        text: String,
        matchResults: List<MatchResult>,
        initialPos: CharPosition,
        finalPos: CharPosition
    ): Token {
        val rule = ruleDrawResolver.resolve(matchResults
            .filter { it.matchType != MatchType.INVALID }
            .map { it.tokenRule })
        return TokenFactory.create(rule, Location(initialPos, finalPos), text)
    }

    private fun containsPartialMatch(matchResults: List<MatchResult>): Boolean =
        matchResults.any {it.matchType == MatchType.PARTIAL }

    private fun areAllMatchResultsInvalid(matchResults: List<MatchResult>): Boolean =
        matchResults.none { it.matchType == MatchType.VALID || it.matchType == MatchType.PARTIAL }

    private fun skipWhitespace(): Optional<Char> {
        var current = reader.read()
        while (current.isPresent) {
            if (!current.get().isWhitespace()) {
                return current
            }
            current = reader.read()
        }
        return Optional.empty()
    }
}
