package printscript.parser.util

import printscript.common.domain.Token
import printscript.parser.error.ParseErrors

fun Token.requireValue(kind: String = "Token"): String =
    value.orElseThrow { ParseErrors.missingValue(this, kind) }