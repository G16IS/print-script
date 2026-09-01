package printscript.formatter.support

import printscript.formatter.PointKind
import printscript.formatter.rules.TokenSpaceRule

fun spaceAroundOperator() =
    TokenSpaceRule(
        tokenType = "OPERATOR",
        kinds = setOf(PointKind.BEFORE_TOKEN, PointKind.AFTER_TOKEN),
        enabled = true,
    )
