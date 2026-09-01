package printscript.rule

import printscript.error.InvalidIdentifierFormat
import printscript.error.LintError
import printscript.syntax.SyntaxNode

class IdentifierFormatRule(
    val letterCase: LetterCase,
) : LintRule {
    override val name: String
        get() = "identifier-format-rule"

    override fun supports(node: SyntaxNode): Boolean = node.name == "variable"

    override fun check(node: SyntaxNode): List<LintError> {
        val idNode = node.childOrNull("ID")
        val identifier = idNode?.token?.value?.orElse(null)
        if (idNode == null || identifier == null || letterCase.matches(identifier)) {
            return emptyList()
        }
        return listOf(
            InvalidIdentifierFormat(
                identifier = identifier,
                expectedFormat = letterCase.formatName,
                location = idNode.location,
            ),
        )
    }
}

// idea extraer esto a una interfaz + implementación que haga esta rule componible de distintos cases
enum class LetterCase {
    CAMEL_CASE,
    SNAKE_CASE,
    ;

    fun matches(identifier: String): Boolean =
        when (this) {
            CAMEL_CASE -> CAMEL_CASE_REGEX.matches(identifier)
            SNAKE_CASE -> SNAKE_CASE_REGEX.matches(identifier)
        }

    val formatName: String
        get() =
            when (this) {
                CAMEL_CASE -> "camelCase"
                SNAKE_CASE -> "snake_case"
            }

    companion object {
        private val CAMEL_CASE_REGEX = Regex("^[a-z][a-zA-Z0-9]*$")
        private val SNAKE_CASE_REGEX = Regex("^[a-z][a-z0-9]*(_[a-z0-9]+)*$")
    }
}
