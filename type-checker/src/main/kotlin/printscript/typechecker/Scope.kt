package printscript.typechecker

data class Scope(
    private val symbols: Map<String, String> = emptyMap(),
) {
    fun declare(
        name: String,
        type: String,
    ): Scope? = if (name in symbols) null else copy(symbols = symbols + (name to type))

    fun lookup(name: String): String? = symbols[name]
}
