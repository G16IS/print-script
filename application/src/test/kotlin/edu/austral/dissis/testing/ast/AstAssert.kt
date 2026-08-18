package edu.austral.dissis.testing.ast

import org.junit.jupiter.api.Assertions.assertEquals
import printscript.syntax.SyntaxNode
import printscript.syntax.SyntaxProgram

fun assertAst(program: SyntaxProgram, init: AstBuilder.() -> Unit) {
    val expected = AstBuilder().apply(init).specs()
    matchProgram(program, expected)
}

private fun matchProgram(program: SyntaxProgram, expected: List<AstSpec>) {
    assertEquals(expected.size, program.statements.size, trees(program, expected))
    program.statements.zip(expected).forEachIndexed { index, pair ->
        matchNode(pair.first, pair.second, "statements[$index]")
    }
}

private fun matchNode(actual: SyntaxNode, expected: AstSpec, path: String) {
    assertEquals(expected.name, actual.name, mismatch(path, "name", actual, expected))
    matchValue(actual, expected, path)
    matchChildren(actual, expected, path)
}

private fun matchValue(actual: SyntaxNode, expected: AstSpec, path: String) {
    if (expected.value == null) return
    assertEquals(expected.value, actual.value(), mismatch(path, "value", actual, expected))
}

private fun matchChildren(actual: SyntaxNode, expected: AstSpec, path: String) {
    assertEquals(
        expected.children.size,
        actual.children.size,
        mismatch(path, "child count", actual, expected)
    )
    actual.children.zip(expected.children).forEachIndexed { index, pair ->
        matchNode(pair.first, pair.second, "$path/${expected.name}[$index]")
    }
}

private fun trees(program: SyntaxProgram, expected: List<AstSpec>): String =
    """
        |tree mismatch
        |actual:
        |${AstRenderer.program(program)}
        |expected:
        |${AstRenderer.specs(expected)}
    """.trimMargin()

private fun mismatch(
    path: String,
    field: String,
    actual: SyntaxNode,
    expected: AstSpec
): String =
    """
        |$field mismatch at $path
        |actual:
        |${AstRenderer.node(actual)}
        |expected:
        |${AstRenderer.spec(expected)}
    """.trimMargin()
