package printscript.rule

import printscript.error.LintError
import printscript.syntax.SyntaxNode

class IdentifierFormatRule(letterCase: LetterCase): LintRule {
    override val name: String
        get() = "identifier-format-rule"

    override fun supports(node: SyntaxNode): Boolean {
        TODO("Not yet implemented")
    }

    override fun check(node: SyntaxNode): List<LintError> {
        TODO("Not yet implemented")
    }
}

enum class LetterCase {
    CAMEL_CASE,
    SNAKE_CASE
}
