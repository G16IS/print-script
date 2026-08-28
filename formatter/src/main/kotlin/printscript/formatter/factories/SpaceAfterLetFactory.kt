package printscript.formatter.factories

import printscript.formatter.FormatError
import printscript.formatter.rules.FormatRule
import printscript.formatter.rules.SpaceAfterLetRule
import printscript.util.Result

object SpaceAfterLetFactory : FormatRuleFactory {
    override val type: String = SpaceAfterLetRule.TYPE
    override val userConfigurable: Boolean = false

    override fun create(params: Map<String, Any?>): Result<FormatRule, FormatError> = Result.Ok(SpaceAfterLetRule)
}
