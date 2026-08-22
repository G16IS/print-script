package printscript.domain

interface SeqStep {
    fun ruleName(): String? = null
}

data class TokenStep(
    val type: String,
    val capture: Boolean,
) : SeqStep

data class RuleRefStep(
    val name: String,
) : SeqStep {
    override fun ruleName(): String = name
}
