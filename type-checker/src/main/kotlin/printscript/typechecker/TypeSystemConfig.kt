package printscript.typechecker

data class TypeSystemConfig(
    val types: List<String>,
    val literals: Map<String, String> = emptyMap(),
    val operations: List<Operation> = emptyList(),
    val nodes: Map<String, NodeConfig> = emptyMap(),
) {
    init {
        validate()
    }

    private fun validate() {
        val known = types.toSet()
        requireKnownTypes(literals.values, known, "literals")
        requireKnownTypes(
            operations.flatMap { it.operands + it.result },
            known,
            "operations",
        )
    }

    private fun requireKnownTypes(
        referenced: Collection<String>,
        known: Set<String>,
        label: String,
    ) {
        val missing = referenced.filter { it !in known }.distinct()
        require(missing.isEmpty()) { "Unknown types in $label: $missing" }
    }
}

data class Operation(
    val op: String,
    val operands: List<String>,
    val result: String,
    val commutative: Boolean = true,
)

data class NodeConfig(
    val kind: String,
    val id: String? = null,
    val declaredType: String? = null,
    val expression: String? = null,
    val callee: String? = null,
    val args: List<String> = emptyList(),
)
