package printscript.domain

interface GrammarRule {
    fun references(): List<String>
}

data class OrRule(val alternatives: List<String>) : GrammarRule {
    override fun references(): List<String> = alternatives
}

data class AtomRule(val token: String) : GrammarRule {
    override fun references(): List<String> = emptyList()
}

data class SeqRule(val steps: List<SeqStep>) : GrammarRule {
    override fun references(): List<String> = steps.mapNotNull { it.ruleName() }
}

data class LeftRule(val left: String, val op: OperatorSpec) : GrammarRule {
    override fun references(): List<String> = listOf(left)
}

data class RepeatRule(val item: String) : GrammarRule {
    override fun references(): List<String> = listOf(item)
}
