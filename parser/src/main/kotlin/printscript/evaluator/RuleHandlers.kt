package printscript.evaluator

object RuleHandlers {
    fun defaults(): List<RuleHandler> = listOf(
        AtomRuleHandler(),
        SeqRuleHandler(),
        OrRuleHandler(),
        LeftRuleHandler(),
        RepeatRuleHandler()
    )
}
