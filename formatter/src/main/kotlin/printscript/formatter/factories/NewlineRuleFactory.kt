package printscript.formatter.factories

import printscript.error.FormatError
import printscript.error.InvalidRuleParams
import printscript.formatter.PointKind
import printscript.formatter.rules.FormatRule
import printscript.formatter.rules.TokenNewlineRule
import printscript.util.Result

object NewlineRuleFactory : FormatRuleFactory {
    const val NEWLINE_BEFORE = "newline-before"
    const val NEWLINE_AFTER = "newline-after"

    override val types: Set<String> = setOf(NEWLINE_BEFORE, NEWLINE_AFTER)

    override fun create(spec: ResolvedFormatRule): Result<FormatRule, FormatError> {
        val kinds = kindsFor(spec.type)
        val count = spec.count ?: TokenNewlineRule.DEFAULT_COUNT

        return when {
            spec.token.isBlank() -> Result.Err(InvalidRuleParams(spec.type, "token no puede estar vacío"))
            kinds == null -> Result.Err(InvalidRuleParams(spec.type, "type de newline desconocido"))
            count !in TokenNewlineRule.ALLOWED_COUNTS ->
                Result.Err(InvalidRuleParams(spec.type, "count debe estar en ${TokenNewlineRule.ALLOWED_COUNTS}"))
            else ->
                Result.Ok(
                    TokenNewlineRule(
                        tokenType = spec.token,
                        kinds = kinds,
                        count = count,
                        tokenValue = spec.value,
                        previousTokenType = spec.previous,
                    ),
                )
        }
    }

    private fun kindsFor(type: String): Set<PointKind>? =
        when (type) {
            NEWLINE_BEFORE -> setOf(PointKind.BEFORE_TOKEN)
            NEWLINE_AFTER -> setOf(PointKind.AFTER_TOKEN)
            else -> null
        }
}
