package printscript

import printscript.syntax.SyntaxProgram

interface Interpreter {
    fun interpret(program: SyntaxProgram): List<SideEffect>
}
