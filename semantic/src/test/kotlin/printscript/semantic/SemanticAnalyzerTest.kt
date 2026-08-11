package printscript.semantic

import printscript.common.VariableType
import printscript.common.ast.BinaryExpression
import printscript.common.ast.CallExpression
import printscript.common.ast.Expression
import printscript.common.ast.ExpressionStatement
import printscript.common.ast.Identifier
import printscript.common.ast.Location
import printscript.common.ast.NumberLiteral
import printscript.common.ast.Program
import printscript.common.ast.Statement
import printscript.common.ast.StringLiteral
import printscript.common.ast.VariableDeclaration
import printscript.common.ast.VariableStatement
import printscript.common.reader.CharPosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SemanticAnalyzerTest {

    @Test
    fun `returns the program when a number declaration is valid`() {
        val program = programOf(
            variable(
                name = "age",
                type = VariableType.NUMBER,
                initializer = number(20.0),
            ),
        )

        val result = SemanticAnalyzer().analyze(program)

        val success = assertIs<SemanticResult.Success>(result)
        assertSame(program, success.program)
    }

    @Test
    fun `returns the program when a string declaration is valid`() {
        val program = programOf(
            variable(
                name = "name",
                type = VariableType.STRING,
                initializer = string("Ada"),
            ),
        )

        val result = SemanticAnalyzer().analyze(program)

        assertIs<SemanticResult.Success>(result)
    }

    @Test
    fun `reports an incompatible initializer type`() {
        val program = programOf(
            variable(
                name = "age",
                type = VariableType.NUMBER,
                initializer = string("twenty"),
            ),
        )

        val failure = assertFailure(SemanticAnalyzer().analyze(program))

        assertEquals(1, failure.errors.size)
        assertTrue(failure.errors.single().messageError.contains("NUMBER"))
        assertTrue(failure.errors.single().messageError.contains("STRING"))
    }

    @Test
    fun `reports a duplicated variable declaration`() {
        val program = programOf(
            variable("age", VariableType.NUMBER, number(20.0)),
            variable("age", VariableType.NUMBER, number(21.0)),
        )

        val failure = assertFailure(SemanticAnalyzer().analyze(program))

        assertEquals(1, failure.errors.size)
        assertTrue(failure.errors.single().messageError.contains("ya fue declarada"))
    }

    @Test
    fun `accepts a reference to a previously declared variable`() {
        val program = programOf(
            variable("age", VariableType.NUMBER, number(20.0)),
            variable("nextAge", VariableType.NUMBER, identifier("age")),
        )

        val result = SemanticAnalyzer().analyze(program)

        assertIs<SemanticResult.Success>(result)
    }

    @Test
    fun `reports a reference to an undeclared variable`() {
        val program = programOf(
            expressionStatement(identifier("missing")),
        )

        val failure = assertFailure(SemanticAnalyzer().analyze(program))

        assertEquals(1, failure.errors.size)
        assertTrue(failure.errors.single().messageError.contains("missing"))
    }

    @Test
    fun `accepts numeric binary expressions`() {
        val sum = binary(number(2.0), "+", number(3.0))
        val multiplication = binary(sum, "*", number(4.0))
        val program = programOf(
            variable("result", VariableType.NUMBER, multiplication),
        )

        val result = SemanticAnalyzer().analyze(program)

        assertIs<SemanticResult.Success>(result)
    }

    @Test
    fun `accepts string concatenation`() {
        val concatenation = binary(string("hello"), "+", string(" world"))
        val program = programOf(
            variable("message", VariableType.STRING, concatenation),
        )

        val result = SemanticAnalyzer().analyze(program)

        assertIs<SemanticResult.Success>(result)
    }

    @Test
    fun `reports incompatible binary operands`() {
        val invalidAddition = binary(number(2.0), "+", string("two"))
        val program = programOf(
            variable("result", VariableType.NUMBER, invalidAddition),
        )

        val failure = assertFailure(SemanticAnalyzer().analyze(program))

        assertEquals(1, failure.errors.size)
        assertTrue(failure.errors.single().messageError.contains("no acepta"))
    }

    @Test
    fun `reports an unknown binary operator`() {
        val unknownOperation = binary(number(2.0), "%", number(2.0))
        val program = programOf(
            variable("result", VariableType.NUMBER, unknownOperation),
        )

        val failure = assertFailure(SemanticAnalyzer().analyze(program))

        assertEquals(1, failure.errors.size)
        assertTrue(failure.errors.single().messageError.contains("Operador desconocido"))
    }

    @Test
    fun `checks expressions used as call arguments`() {
        val call = CallExpression(
            callee = "println",
            args = listOf(identifier("missing")),
            location = location,
        )
        val program = programOf(expressionStatement(call))

        val failure = assertFailure(SemanticAnalyzer().analyze(program))

        assertEquals(1, failure.errors.size)
        assertTrue(failure.errors.single().messageError.contains("missing"))
    }

    @Test
    fun `does not share symbols between analyses`() {
        val analyzer = SemanticAnalyzer()
        val firstProgram = programOf(
            variable("age", VariableType.NUMBER, number(20.0)),
        )
        val secondProgram = programOf(
            variable("age", VariableType.NUMBER, number(30.0)),
        )

        val firstResult = analyzer.analyze(firstProgram)
        val secondResult = analyzer.analyze(secondProgram)

        assertIs<SemanticResult.Success>(firstResult)
        assertIs<SemanticResult.Success>(secondResult)
    }

    private fun assertFailure(result: SemanticResult): SemanticResult.Failure =
        assertIs<SemanticResult.Failure>(result)

    private fun programOf(vararg statements: Statement): Program =
        Program(
            statements = statements.toList(),
            location = location,
        )

    private fun variable(
        name: String,
        type: VariableType,
        initializer: Expression,
    ): VariableStatement {
        val declaration = VariableDeclaration(
            id = identifier(name),
            typeAnnotation = type,
            initializer = initializer,
            location = location,
        )

        return VariableStatement(
            declaration = declaration,
            location = location,
        )
    }

    private fun expressionStatement(expression: Expression): ExpressionStatement =
        ExpressionStatement(
            expression = expression,
            location = location,
        )

    private fun identifier(name: String): Identifier =
        Identifier(
            name = name,
            location = location,
        )

    private fun number(value: Double): NumberLiteral =
        NumberLiteral(
            value = value,
            location = location,
        )

    private fun string(value: String): StringLiteral =
        StringLiteral(
            value = value,
            location = location,
        )

    private fun binary(
        left: Expression,
        operation: String,
        right: Expression,
    ): BinaryExpression =
        BinaryExpression(
            left = left,
            right = right,
            operation = operation,
            location = location,
        )

    private companion object {
        val location = Location(
            start = CharPosition(1, 1),
            end = CharPosition(1, 1),
        )
    }
}
