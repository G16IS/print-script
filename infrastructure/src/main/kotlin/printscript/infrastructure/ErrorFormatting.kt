package printscript.infrastructure

import printscript.ast.Location
import printscript.error.FormatError
import printscript.error.LintError
import printscript.error.RuntimeError
import printscript.typechecker.TypeError

internal fun formatLocated(
    message: String,
    location: Location,
): String = "$message (${location.start.line}:${location.start.col}-${location.end.line}:${location.end.col})"

internal fun formatTypeError(error: TypeError): String = formatLocated(error.message, error.location)

internal fun formatLintError(error: LintError): String = formatLocated(error.message, error.location)

internal fun formatFormatError(error: FormatError): String = formatLocated(error.message, error.location)

internal fun formatRuntimeError(error: RuntimeError): String = formatLocated(error.message, error.location)
