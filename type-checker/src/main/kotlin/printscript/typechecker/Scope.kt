package printscript.typechecker

data class Symbol(
    val type: String,
    val mutable: Boolean = true,
)

data class Scope(
    private val symbols: Map<String, Symbol> = emptyMap(),
) {
    fun declare(
        name: String,
        type: String,
        mutable: Boolean = true,
    ): Scope? = if (name in symbols) null else copy(symbols = symbols + (name to Symbol(type, mutable)))

    fun lookup(name: String): String? = symbols[name]?.type

    fun lookupSymbol(name: String): Symbol? = symbols[name]
}
