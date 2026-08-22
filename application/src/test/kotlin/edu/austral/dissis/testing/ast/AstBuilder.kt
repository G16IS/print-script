package edu.austral.dissis.testing.ast

class AstBuilder {
    private val nodes = mutableListOf<AstSpec>()

    fun node(
        name: String,
        value: String? = null,
        init: AstBuilder.() -> Unit = {},
    ) {
        val nested = AstBuilder()
        nested.init()
        nodes += AstSpec(name, value, nested.nodes.toList())
    }

    fun specs(): List<AstSpec> = nodes.toList()
}
