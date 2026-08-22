package printscript.evaluator

import printscript.domain.TokenRule

data class MatchResult(
    val tokenRule: TokenRule,
    val matchType: MatchType,
    val category: String,
)
