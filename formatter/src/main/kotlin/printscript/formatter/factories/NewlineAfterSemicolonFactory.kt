package printscript.formatter.factories

import printscript.formatter.FormatError
import printscript.formatter.rules.FormatRule
import printscript.formatter.rules.NewlineAfterSemicolonRule
import printscript.util.Result

object NewlineAfterSemicolonFactory : FormatRuleFactory {
    override val type: String = NewlineAfterSemicolonRule.TYPE
    override val userConfigurable: Boolean = false

    override fun create(params: Map<String, Any?>): Result<FormatRule, FormatError> =
        Result.Ok(NewlineAfterSemicolonRule)
}
