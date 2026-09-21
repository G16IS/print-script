package printscript.parser.parse

import printscript.error.ParserError

/**
 * Outcome of evaluating a grammar rule.
 *
 * [Matched] carries the produced node. [Missing] means the rule did not match and is
 * backtrackable: the token cursor must be restored before trying alternatives. [Failed]
 * is an unrecoverable error: the tokens cannot match the rule at all, so parsing aborts.
 */
sealed interface ParseResult<out T> {
    data class Matched<T>(
        val node: T,
    ) : ParseResult<T>

    data object Missing : ParseResult<Nothing>

    data class Failed(
        val error: ParserError,
    ) : ParseResult<Nothing>
}
