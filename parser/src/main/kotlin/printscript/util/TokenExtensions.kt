package printscript.util

import printscript.domain.Token
import printscript.error.ParseErrors

fun Token.requireValue(kind: String = "Token"): String =
    value.orElseThrow { ParseErrors.missingValue(this, kind) }