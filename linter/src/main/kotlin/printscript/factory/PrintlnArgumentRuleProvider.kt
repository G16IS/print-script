package printscript.factory

import printscript.rule.LintRule
import printscript.rule.PrintlnArgumentRule

class PrintlnArgumentRuleProvider : LintRuleProvider {
    override val id: String = "println-simple-argument"

    override fun create(options: Map<String, String>): LintRule {
        val callee = options["callee"] ?: "println"
        return PrintlnArgumentRule(callee)
    }
}
