package printscript.parse

object RuleHandlers {
    fun v1(): List<RuleHandler> =
        listOf(
            AtomRuleHandler(),
            SeqRuleHandler(),
            OrRuleHandler(),
            LeftRuleHandler(),
            RepeatRuleHandler(),
        )
}
