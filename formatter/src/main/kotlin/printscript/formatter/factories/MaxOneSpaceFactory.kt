package printscript.formatter.factories

import printscript.formatter.FormatError
import printscript.formatter.rules.FormatRule
import printscript.formatter.rules.MaxOneSpaceRule
import printscript.util.Result

object MaxOneSpaceFactory : FormatRuleFactory {
    override val type: String = MaxOneSpaceRule.TYPE
    override val userConfigurable: Boolean = false

    override fun create(params: Map<String, Any?>): Result<FormatRule, FormatError> = Result.Ok(MaxOneSpaceRule)
}
