package printscript.error

import printscript.syntax.Location

/**
 * Formato único de error para todas las salidas: CLI y entrypoints del TCK.
 * La consigna pide fila y columna de inicio y fin.
 */
fun formatLocated(
    message: String,
    location: Location,
): String = "$message (${location.start.line}:${location.start.col}-${location.end.line}:${location.end.col})"

fun formatError(error: Error): String =
    when (error) {
        is FormatError -> formatFormatError(error)
        is LexerError -> formatLocated(error.message, error.location)
        is LintError -> formatLintError(error)
        is ParserError -> formatLocated(error.message, error.location)
        is RuntimeError -> formatRuntimeError(error)
        is TypeError -> formatTypeError(error)
    }

fun formatTypeError(error: TypeError): String = formatLocated(error.message, error.location)

fun formatLintError(error: LintError): String = formatLocated(error.message, error.location)

fun formatFormatError(error: FormatError): String = formatLocated(error.message, error.location)

fun formatRuntimeError(error: RuntimeError): String = formatLocated(error.message, error.location)
