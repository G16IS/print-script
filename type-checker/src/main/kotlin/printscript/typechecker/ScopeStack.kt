package printscript.typechecker

data class ScopeStack(
    private val scopes: List<Scope> = listOf(Scope()),
) {
    fun push(): ScopeStack = copy(scopes = scopes + Scope())

    fun pop(): ScopeStack = if (scopes.size <= 1) this else copy(scopes = scopes.dropLast(1))

    fun declare(
        name: String,
        type: String,
    ): ScopeStack? {
        val top = scopes.last().declare(name, type) ?: return null
        return copy(scopes = scopes.dropLast(1) + top)
    }

    fun lookup(name: String): String? = scopes.asReversed().firstNotNullOfOrNull { it.lookup(name) }
}
