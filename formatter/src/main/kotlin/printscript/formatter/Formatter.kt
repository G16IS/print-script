package printscript.formatter

import printscript.syntax.SyntaxProgram
import printscript.util.Report
import printscript.util.Result

interface Formatter {
    fun format(program: SyntaxProgram): Result<String, FormatError>

    fun check(
        program: SyntaxProgram,
        source: String,
    ): Report<Unit, FormatError>
}
