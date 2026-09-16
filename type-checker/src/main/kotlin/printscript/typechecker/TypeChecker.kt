package printscript.typechecker

import printscript.syntax.SyntaxNode
import printscript.syntax.SyntaxProgram
import printscript.typechecker.handlers.StatementCheck
import printscript.util.Report
import printscript.util.Result

interface TypeChecker {
    fun check(program: SyntaxProgram): Report<SyntaxProgram, TypeError>

    fun checkStrict(program: SyntaxProgram): Result<SyntaxProgram, TypeError>

    fun checkStatement(
        statement: SyntaxNode,
        scope: ScopeStack,
    ): StatementCheck
}
