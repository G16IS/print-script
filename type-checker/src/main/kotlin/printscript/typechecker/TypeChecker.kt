package printscript.typechecker

import printscript.syntax.SyntaxProgram
import printscript.util.Report
import printscript.util.Result

interface TypeChecker {
    fun check(program: SyntaxProgram): Report<SyntaxProgram, TypeError>

    fun checkStrict(program: SyntaxProgram): Result<SyntaxProgram, TypeError>
}
