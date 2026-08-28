package printscript.formatter.factories

import printscript.formatter.FormatError
import printscript.formatter.PointKind
import printscript.formatter.rules.FormatRule
import printscript.util.Result

object SpaceAroundAssignFactory : FormatRuleFactory {
    const val TYPE = "space-around-assign"

    override val type: String = TYPE
    override val userConfigurable: Boolean = true

    override fun create(params: Map<String, Any?>): Result<FormatRule, FormatError> =
        enabledSpaceRule(
            TYPE,
            "ASSIGN",
            setOf(PointKind.BEFORE_TOKEN, PointKind.AFTER_TOKEN),
            params,
        )
}
