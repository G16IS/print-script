package printscript.typechecker

data class Symbol(
    val type: String,
    val mutable: Boolean = true,
)
