package printscript.formatter.factories

import printscript.formatter.FormatError
import printscript.formatter.PointKind
import printscript.formatter.rules.FormatRule
import printscript.util.Result

object SpaceBeforeColonFactory : FormatRuleFactory {
    const val TYPE = "space-before-colon"

    override val type: String = TYPE
    override val userConfigurable: Boolean = true

    override fun create(params: Map<String, Any?>): Result<FormatRule, FormatError> =
        enabledSpaceRule(TYPE, "COLON", setOf(PointKind.BEFORE_TOKEN), params)
}
