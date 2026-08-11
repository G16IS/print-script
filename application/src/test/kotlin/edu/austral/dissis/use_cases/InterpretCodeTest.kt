package edu.austral.dissis.use_cases

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InterpretCodeTest {

    @Test
    fun `declarations and prints produce expected program text`() {
        val program = interpretCodeFromResource("examples/declarations_and_prints.ps")

        val expected = """
            Program(statements=[VariableStatement(declaration=VariableDeclaration(id=Identifier(name=pepe, location=Location(start=CharPosition(line=1, col=6), end=CharPosition(line=1, col=9))), typeAnnotation=STRING, initializer=StringLiteral(value=Hello, World!, location=Location(start=CharPosition(line=1, col=21), end=CharPosition(line=1, col=35))), location=Location(start=CharPosition(line=1, col=2), end=CharPosition(line=1, col=36))), location=Location(start=CharPosition(line=1, col=2), end=CharPosition(line=1, col=36))), VariableStatement(declaration=VariableDeclaration(id=Identifier(name=pepa, location=Location(start=CharPosition(line=2, col=6), end=CharPosition(line=2, col=9))), typeAnnotation=NUMBER, initializer=NumberLiteral(value=42.0, location=Location(start=CharPosition(line=2, col=21), end=CharPosition(line=2, col=22))), location=Location(start=CharPosition(line=2, col=2), end=CharPosition(line=2, col=23))), location=Location(start=CharPosition(line=2, col=2), end=CharPosition(line=2, col=23))), ExpressionStatement(expression=CallExpression(callee=println, args=[Identifier(name=pepe, location=Location(start=CharPosition(line=4, col=10), end=CharPosition(line=4, col=13)))], location=Location(start=CharPosition(line=4, col=2), end=CharPosition(line=4, col=15))), location=Location(start=CharPosition(line=4, col=2), end=CharPosition(line=4, col=15))), ExpressionStatement(expression=CallExpression(callee=println, args=[Identifier(name=pepa, location=Location(start=CharPosition(line=5, col=10), end=CharPosition(line=5, col=13)))], location=Location(start=CharPosition(line=5, col=2), end=CharPosition(line=5, col=15))), location=Location(start=CharPosition(line=5, col=2), end=CharPosition(line=5, col=15)))], location=Location(start=CharPosition(line=0, col=0), end=CharPosition(line=5, col=15)))
        """.trimIndent()

        assertEquals(expected, program.toString())
    }

    @Test
    fun `binary expression produces expected program text`() {
        val program = interpretCodeFromResource("examples/binary_expression.ps")

        val expected = """
            Program(statements=[VariableStatement(declaration=VariableDeclaration(id=Identifier(name=x, location=Location(start=CharPosition(line=1, col=6), end=CharPosition(line=1, col=6))), typeAnnotation=NUMBER, initializer=BinaryExpression(left=NumberLiteral(value=1.0, location=Location(start=CharPosition(line=1, col=18), end=CharPosition(line=1, col=18))), right=BinaryExpression(left=NumberLiteral(value=2.0, location=Location(start=CharPosition(line=1, col=22), end=CharPosition(line=1, col=22))), right=NumberLiteral(value=3.0, location=Location(start=CharPosition(line=1, col=26), end=CharPosition(line=1, col=26))), operation=*, location=Location(start=CharPosition(line=1, col=22), end=CharPosition(line=1, col=26))), operation=+, location=Location(start=CharPosition(line=1, col=18), end=CharPosition(line=1, col=26))), location=Location(start=CharPosition(line=1, col=2), end=CharPosition(line=1, col=27))), location=Location(start=CharPosition(line=1, col=2), end=CharPosition(line=1, col=27))), ExpressionStatement(expression=CallExpression(callee=println, args=[Identifier(name=x, location=Location(start=CharPosition(line=2, col=10), end=CharPosition(line=2, col=10)))], location=Location(start=CharPosition(line=2, col=2), end=CharPosition(line=2, col=12))), location=Location(start=CharPosition(line=2, col=2), end=CharPosition(line=2, col=12)))], location=Location(start=CharPosition(line=0, col=0), end=CharPosition(line=2, col=12)))
        """.trimIndent()

        assertEquals(expected, program.toString())
    }

    @Test
    fun `string literal produces expected program text`() {
        val program = interpretCodeFromResource("examples/string_literal.ps")

        val expected = """
            Program(statements=[VariableStatement(declaration=VariableDeclaration(id=Identifier(name=greeting, location=Location(start=CharPosition(line=1, col=6), end=CharPosition(line=1, col=13))), typeAnnotation=STRING, initializer=StringLiteral(value=hola, location=Location(start=CharPosition(line=1, col=25), end=CharPosition(line=1, col=30))), location=Location(start=CharPosition(line=1, col=2), end=CharPosition(line=1, col=31))), location=Location(start=CharPosition(line=1, col=2), end=CharPosition(line=1, col=31))), ExpressionStatement(expression=CallExpression(callee=println, args=[Identifier(name=greeting, location=Location(start=CharPosition(line=2, col=10), end=CharPosition(line=2, col=17)))], location=Location(start=CharPosition(line=2, col=2), end=CharPosition(line=2, col=19))), location=Location(start=CharPosition(line=2, col=2), end=CharPosition(line=2, col=19)))], location=Location(start=CharPosition(line=0, col=0), end=CharPosition(line=2, col=19)))
        """.trimIndent()

        assertEquals(expected, program.toString())
    }
}
