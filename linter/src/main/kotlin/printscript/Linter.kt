package printscript

import printscript.error.LintError
import printscript.syntax.SyntaxProgram
import printscript.util.Report

interface Linter {
    fun lint(program: SyntaxProgram): Report<SyntaxProgram, LintError>
}
