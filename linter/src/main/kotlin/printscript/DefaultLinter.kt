package printscript

import printscript.error.LintError
import printscript.rule.LintRule
import printscript.syntax.SyntaxNode
import printscript.syntax.SyntaxProgram
import printscript.util.Report
import printscript.walk.SyntaxTreeWalker

class DefaultLinter(
    private val lintingRules: List<LintRule> = emptyList(),
) : Linter {
    override fun lint(program: SyntaxProgram): Report<SyntaxProgram, LintError> {
        val errors = mutableListOf<LintError>()

        for (statement in program.statements) {
            errors += lint(statement)
        }

        return Report(program, errors)
    }

    private fun lint(node: SyntaxNode): List<LintError> {
        val errors = mutableListOf<LintError>()
        val nodes = SyntaxTreeWalker.walk(node)

        for (currentNode in nodes) {
            for (rule in lintingRules) {
                if (rule.supports(currentNode)) {
                    errors += rule.check(currentNode)
                }
            }
        }

        return errors.sortedWith(
            compareBy(
                { it.location.start.line },
                { it.location.start.col },
            ),
        )
    }
}
