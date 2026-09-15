package printscript

import java.util.Optional
import printscript.domain.Token
import printscript.error.LexerError
import printscript.error.UnexpectedEnfOfLine
import printscript.error.UnexpectedToken
import printscript.evaluator.MatchResult
import printscript.evaluator.MatchType
import printscript.evaluator.RuleEvaluator
import printscript.reader.CharPosition
import printscript.reader.CodeReader
import printscript.syntax.Location
import printscript.util.Result

class TokenStream(
    private val reader: CodeReader,
    val ruleEvaluator: RuleEvaluator,
    val ruleDrawResolver: RuleDrawResolver,
) : Lexer {
    private val buffer = ArrayDeque<Token>()

    override fun nextToken(): Result<Token, LexerError> {
        val token = buffer.removeFirstOrNull() ?: return readNextToken()

        return Result.Ok(token)
    }

    override fun peek(offset: Int?): Result<Token, LexerError> {
        val realOffset: Int = offset ?: 0

        while (buffer.size <= realOffset) {
            val nextToken: Result<Token, LexerError> = readNextToken()

            when (nextToken) {
                is Result.Ok -> buffer.addLast(nextToken.value)
                is Result.Err -> return nextToken
            }
        }
        return Result.Ok(buffer[realOffset])
    }

    private fun readNextToken(): Result<Token, LexerError> {
        val first = skipWhitespace()
        if (first.isEmpty) {
            val pos = reader.currentPosition()
            return Result.Ok(Token("EOF", Optional.empty(), Location(pos, pos)))
            // TODO-future: cambiar por TerminalToken
        }

        return scanToken(first.get().toString(), reader.currentPosition())
    }

    private fun scanToken(
        initialText: String,
        initialPos: CharPosition,
    ): Result<Token, LexerError> {
        var text = initialText
        var lastMatchResults = ruleEvaluator.evaluate(text)

        if (areAllMatchResultsInvalid(lastMatchResults)) {
            return Result.Err(UnexpectedToken(Location(initialPos, initialPos)))
        }

        var result: Result<Token, LexerError>? = null
        while (result == null) {
            val nextChar = reader.peek()

            result =
                if (nextChar.isEmpty) {
                    resolveEndOfInput(text, lastMatchResults, initialPos)
                } else {
                    val matchResults = ruleEvaluator.evaluate(text + nextChar.get())
                    if (areAllMatchResultsInvalid(matchResults)) {
                        buildToken(text, lastMatchResults, initialPos, reader.currentPosition())
                    } else {
                        reader.read()
                        text += nextChar.get()
                        lastMatchResults = matchResults
                        null
                    }
                }
        }

        return result
    }

    private fun resolveEndOfInput(
        text: String,
        lastMatchResults: List<MatchResult>,
        initialPos: CharPosition,
    ): Result<Token, LexerError> =
        if (lastMatchResults.any { it.matchType == MatchType.VALID }) {
            buildToken(text, lastMatchResults, initialPos, reader.currentPosition())
        } else {
            Result.Err(UnexpectedEnfOfLine(Location(reader.currentPosition(), reader.currentPosition()), text))
        }

    /**
     * Sólo los matches **completos** son candidatos a emitir.
     *
     * Un PARTIAL sirve para seguir consumiendo caracteres, no para ganar el
     * desempate por prioridad de categoría: con `let n: number`, el lexema `n`
     * es PARTIAL para el `TYPE` `number` y VALID para `ID`, y `types` gana en
     * `order`. Si se lo dejara competir, `n` se emitiría como `TYPE`.
     */
    private fun buildToken(
        text: String,
        matchResults: List<MatchResult>,
        initialPos: CharPosition,
        finalPos: CharPosition,
    ): Result<Token, LexerError> {
        val valid = matchResults.filter { it.matchType == MatchType.VALID }
        if (valid.isEmpty()) {
            return Result.Err(UnexpectedToken(Location(initialPos, finalPos)))
        }

        val rule = ruleDrawResolver.resolve(valid.map { it.tokenRule })

        when (rule) {
            is Result.Err -> return rule
            is Result.Ok -> return TokenFactory.create(rule.value, Location(initialPos, finalPos), text)
        }
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
