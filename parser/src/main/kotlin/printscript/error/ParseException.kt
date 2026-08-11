package printscript.error

import printscript.ast.Location

/**
 * Thrown when the token stream does not match the expected grammar.
 * Semantic issues (undeclared vars, type mismatches) are out of scope.
 */
class ParseException(
    message: String,
    val location: Location
) : RuntimeException(formatMessage(message, location)) {
    companion object {
        private fun formatMessage(message: String, location: Location): String =
            "$message at line ${location.start.line}, column ${location.start.col}"
    }
}
