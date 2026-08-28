package printscript.formatter.factories

import printscript.formatter.FormatError
import printscript.formatter.rules.FormatRule
import printscript.formatter.rules.NewlinesBeforePrintlnRule
import printscript.util.Result
import printscript.util.map

object NewlinesBeforePrintlnFactory : FormatRuleFactory {
    override val type: String = NewlinesBeforePrintlnRule.TYPE
    override val userConfigurable: Boolean = true

    override fun create(params: Map<String, Any?>): Result<FormatRule, FormatError> =
        newlineCount(
            NewlinesBeforePrintlnRule.TYPE,
            params,
            NewlinesBeforePrintlnRule.ALLOWED_COUNTS,
            NewlinesBeforePrintlnRule.DEFAULT_COUNT,
        ).map { count -> NewlinesBeforePrintlnRule(count) }
}
