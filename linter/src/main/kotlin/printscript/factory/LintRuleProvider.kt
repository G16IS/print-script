package printscript.factory

import printscript.rule.LintRule

interface LintRuleProvider {
    val id: String

    fun create(options: Map<String, String>): LintRule
}
