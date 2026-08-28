package printscript.formatter.factories

import printscript.formatter.FormatError
import printscript.formatter.InvalidRuleParams
import printscript.formatter.PointKind
import printscript.formatter.rules.FormatRule
import printscript.formatter.rules.TokenSpaceRule
import printscript.util.Result

internal fun enabledSpaceRule(
    type: String,
    tokenType: String,
    kinds: Set<PointKind>,
    params: Map<String, Any?>,
): Result<FormatRule, FormatError> {
    val enabled =
        when (val value = params["enabled"]) {
            null -> true
            is Boolean -> value
            else -> return Result.Err(InvalidRuleParams(type, "enabled debe ser boolean"))
        }

    return Result.Ok(TokenSpaceRule(tokenType, kinds, enabled))
}

internal fun newlineCount(
    type: String,
    params: Map<String, Any?>,
    allowed: IntRange,
    default: Int,
): Result<Int, FormatError> {
    val count =
        when (val value = params["count"]) {
            null -> default
            is Int -> value
            is Number -> value.toInt()
            else -> return Result.Err(InvalidRuleParams(type, "count debe ser un entero"))
        }

    return if (count in allowed) {
        Result.Ok(count)
    } else {
        Result.Err(InvalidRuleParams(type, "count debe estar en $allowed"))
    }
}
