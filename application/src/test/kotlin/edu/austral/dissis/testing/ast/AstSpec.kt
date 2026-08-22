package edu.austral.dissis.testing.ast

data class AstSpec(
    val name: String,
    val value: String? = null,
    val children: List<AstSpec> = emptyList(),
)
