package printscript

import printscript.error.LintError
import printscript.syntax.SyntaxProgram
import printscript.util.Report

class DefaultLinter(
    lintingRules: List<LintViolation>,
) : Linter {
    override fun lint(program: SyntaxProgram): Report<SyntaxProgram, LintError> {
        TODO("Not yet implemented")
    }
}
