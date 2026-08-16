package printscript

import printscript.ast.Location
import printscript.domain.Token
import printscript.reader.CharPosition
import printscript.reader.CodeReader
import java.util.Optional
import printscript.evaluator.MatchResult
import printscript.evaluator.MatchType
import printscript.evaluator.RuleEvaluator

class TokenStream(private val reader: CodeReader, val ruleEvaluator: RuleEvaluator) : Lexer {
    private val buffer = ArrayDeque<Token>()
    private val lastMatchResults: List<MatchResult>? = null

    override fun nextToken(): Token = buffer.removeFirstOrNull() ?: readNextToken()
    override fun peek(offset: Int?): Token {
        val realOffset: Int = offset ?: 0

        while (buffer.size <= realOffset) buffer.addLast(readNextToken())
        return buffer[realOffset]
    }

    private fun readNextToken(): Token {
        var text: String = ""
        while (true) {
            val nextChar = reader.peek()
            val initialPos = reader.currentPosition()
            if (nextChar.isEmpty) {
                if (text.isEmpty()) return Token(
                    "EOF", Optional.empty(),
                    Location(initialPos, initialPos)
                )
                else throw Error("Unexpected token at: line ${initialPos.col} col ${initialPos.col}")
            }
            val matchResults: List<MatchResult> = ruleEvaluator.evaluate(text)
            if (areAllMatchResultsInvalid(matchResults)) {
                
            }
        }
    }

    private fun handleEmptyChar(text: String, location: Location){

    }

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
