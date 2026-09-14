package printscript.error

import printscript.domain.TokenRule
import printscript.reader.CharPosition
import printscript.syntax.Location

sealed interface LexerError : Error {
    override val message: String
    val location: Location
}

data class UnexpectedToken(
    override val location: Location,
) : LexerError {
    override val message: String
        get() = "Unexpected token at line ${location.start.line} col ${location.start.col}"
}

data class UnexpectedEnfOfLine(
    override val location: Location,
    val wasReadingToken: String,
) : LexerError {
    override val message: String
        get() =
            "Unexpected end of file at line " +
                "${location.start.line} col " +
                "${location.start.col} " +
                "while reading token $wasReadingToken"
}

data class MultipleRulesWithSamePriority(
    override val location: Location = Location(CharPosition(0, 0), CharPosition(0, 0)),
    val rules: List<TokenRule>,
) : LexerError {
    override val message: String
        get() = "Multiple rules found with the same priority: $rules"
}

data class NoRulesProvided(
    override val location: Location = Location(CharPosition(0, 0), CharPosition(0, 0)),
) : LexerError {
    override val message: String
        get() = "No matching rules provided"
}

data class RuleNotFound(
    override val location: Location = Location(CharPosition(0, 0), CharPosition(0, 0)),
    val rule: TokenRule,
) : LexerError {
    override val message: String
        get() = "Rule $rule not found in config"
}
