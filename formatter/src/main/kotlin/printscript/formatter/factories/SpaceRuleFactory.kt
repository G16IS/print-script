package printscript.formatter.factories

import printscript.formatter.FormatError
import printscript.formatter.InvalidRuleParams
import printscript.formatter.PointKind
import printscript.formatter.rules.FormatRule
import printscript.formatter.rules.TokenSpaceRule
import printscript.util.Result

object SpaceRuleFactory : FormatRuleFactory {
    const val SPACE_BEFORE = "space-before"
    const val SPACE_AFTER = "space-after"
    const val SPACE_AROUND = "space-around"

    override val types: Set<String> = setOf(SPACE_BEFORE, SPACE_AFTER, SPACE_AROUND)

    override fun create(spec: ResolvedFormatRule): Result<FormatRule, FormatError> =
        when {
            spec.token.isBlank() -> Result.Err(InvalidRuleParams(spec.type, "token no puede estar vacío"))
            else ->
                kindsFor(spec.type)
                    ?.let { kinds -> Result.Ok(TokenSpaceRule(spec.token, kinds, spec.enabled ?: true)) }
                    ?: Result.Err(InvalidRuleParams(spec.type, "type de espacio desconocido"))
        }

    private fun kindsFor(type: String): Set<PointKind>? =
        when (type) {
            SPACE_BEFORE -> setOf(PointKind.BEFORE_TOKEN)
            SPACE_AFTER -> setOf(PointKind.AFTER_TOKEN)
            SPACE_AROUND -> setOf(PointKind.BEFORE_TOKEN, PointKind.AFTER_TOKEN)
            else -> null
        }
}
