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
import printscript.util.err
import printscript.util.flatMap
import printscript.util.fold
import printscript.util.ok

class TokenStream(
    private val reader: CodeReader,
    private val ruleEvaluator: RuleEvaluator,
    private val ruleDrawResolver: RuleDrawResolver,
) : Lexer {
    private val buffer = ArrayDeque<Token>()

    companion object {
        const val END_TOKEN = "EOF"
    }

    fun getTerminalToken(position: CharPosition) =
        Token(
            END_TOKEN,
            Optional.empty(),
            Location(position, position),
        )

    override fun nextToken(): Result<Token, LexerError> {
        val token =
            buffer.removeFirstOrNull()
                ?: return readNextToken()

        return ok(token)
    }

    override fun peek(offset: Int): Result<Token, LexerError> {
        while (buffer.size <= offset) {
            readNextToken()
                .fold(
                    onOk = { buffer.addLast(it) },
                    onErr = { return err(it) },
                )
        }

        return ok(buffer[offset])
    }

    private fun readNextToken(): Result<Token, LexerError> {
        val first = skipWhitespace()
        val initialPos = reader.currentPosition()

        if (first.isEmpty) {
            return ok(getTerminalToken(initialPos))
        }

        val firstMatchResults = ruleEvaluator.evaluate(first.get())

        if (areAllMatchResultsInvalid(firstMatchResults)) {
            return err(UnexpectedToken(Location.point(initialPos)))
        }

        return consumeToken(first.get(), firstMatchResults, initialPos)
    }

    /**
     * Mientras siga matcheando alguna regla ("maximal munch").
     * Corta cuando se acaba el input ([resolveEndOfInput]) o el próximo
     * carácter invalida todos los matches ([buildToken]).
     */
    private tailrec fun consumeToken(
        text: String,
        lastMatchResults: List<MatchResult>,
        initialPos: CharPosition,
    ): Result<Token, LexerError> {
        val nextChar = reader.peek()
        val matchResults = nextChar.map { ruleEvaluator.evaluate(text + it) }

        return when {
            nextChar.isEmpty -> resolveEndOfInput(text, lastMatchResults, initialPos)

            areAllMatchResultsInvalid(matchResults.get()) ->
                buildToken(text, lastMatchResults, initialPos, reader.currentPosition())

            else -> {
                reader.read()
                consumeToken(text + nextChar.get(), matchResults.get(), initialPos)
            }
        }
    }

    private fun resolveEndOfInput(
        text: String,
        lastMatchResults: List<MatchResult>,
        initialPos: CharPosition,
    ): Result<Token, LexerError> {
        val currentPosition = reader.currentPosition()

        if (!isSomeMatchResultValid(lastMatchResults)) {
            return err(UnexpectedEnfOfLine(Location.point(currentPosition), text))
        }

        return buildToken(text, lastMatchResults, initialPos, currentPosition)
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
        val location = Location(initialPos, finalPos)

        val valid = getValidMatchResults(matchResults)
        if (valid.isEmpty()) {
            return err(UnexpectedToken(location))
        }

        return ruleDrawResolver
            .resolve(valid.map { it.tokenRule })
            .flatMap { rule -> TokenFactory.create(rule, location, text) }
    }

    private fun skipWhitespace(): Optional<String> {
        var current = reader.read()

        while (current.isPresent) {
            if (!current.get().isWhitespace()) {
                return Optional.of(current.get().toString())
            }

            current = reader.read()
        }

        return Optional.empty()
    }

    // == Helpers ==

    private fun getValidMatchResults(matchResults: List<MatchResult>) =
        matchResults
            .filter { it.matchType == MatchType.VALID }

    private fun isSomeMatchResultValid(matchResults: List<MatchResult>): Boolean =
        matchResults
            .any { it.matchType == MatchType.VALID }

    private fun areAllMatchResultsInvalid(matchResults: List<MatchResult>): Boolean =
        matchResults
            .none { it.matchType == MatchType.VALID || it.matchType == MatchType.PARTIAL }
}
