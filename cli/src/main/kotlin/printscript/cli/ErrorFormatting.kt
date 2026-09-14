package printscript.cli

import printscript.error.Error
import printscript.error.ExecutionFailure
import printscript.error.FormatError
import printscript.error.LexerError
import printscript.error.LintError
import printscript.error.ParserError
import printscript.error.RuntimeError
import printscript.error.TypeError
import printscript.syntax.Location

internal fun formatLocated(
    message: String,
    location: Location,
): String = "$message (${location.start.line}:${location.start.col}-${location.end.line}:${location.end.col})"

internal fun formatError(error: Error): String =
    when (error) {
        is FormatError -> formatFormatError(error)
        is LexerError -> formatLocated(error.message, error.location)
        is LintError -> formatLintError(error)
        is ParserError -> formatLocated(error.message, error.location)
        is RuntimeError -> formatRuntimeError(error)
        is TypeError -> formatTypeError(error)
        is ExecutionFailure.Runtime -> formatRuntimeError(error.error)
        is ExecutionFailure.Types -> error.errors.joinToString("\n", transform = ::formatError)
    }

internal fun formatTypeError(error: TypeError): String = formatLocated(error.message, error.location)

internal fun formatLintError(error: LintError): String = formatLocated(error.message, error.location)

internal fun formatFormatError(error: FormatError): String = formatLocated(error.message, error.location)

internal fun formatRuntimeError(error: RuntimeError): String = formatLocated(error.message, error.location)
