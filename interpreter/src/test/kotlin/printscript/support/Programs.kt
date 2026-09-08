package printscript.support

import printscript.syntax.SyntaxNode
import printscript.syntax.SyntaxProgram

fun program(vararg statements: SyntaxNode): SyntaxProgram = SyntaxProgram(statements.toList(), TEST_LOCATION)
