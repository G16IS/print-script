package printscript.parser.parse

object RuleHandlers {
    fun defaults(): List<RuleHandler> =
        listOf(
            AtomRuleHandler(),
            SeqRuleHandler(),
            OrRuleHandler(),
            LeftRuleHandler(),
            RepeatRuleHandler(),
            OptionalRuleHandler(),
            TryRuleHandler(),
        )
}
