package printscript.typechecker

import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import printscript.domain.NodeConfig
import printscript.domain.Operation
import printscript.domain.Token
import printscript.domain.TypeSystemConfig
import printscript.reader.CharPosition
import printscript.syntax.Location
import printscript.syntax.SyntaxNode
import printscript.syntax.SyntaxProgram
import printscript.util.Result

class TypeCheckerTest {
    private val location = Location(CharPosition(1, 1), CharPosition(1, 2))
    private val config = canonicalConfig()
    private val checker = DefaultTypeCheckerFactory.create(config)

    @Test
    fun `number declaration matches the initializer`() {
        val program = program(variable("x", "number", numberExpr("1")))

        val report = checker.check(program)

        assertTrue(report.isOk)
        assertEquals(program, report.value)
        assertEquals(Result.Ok(program), checker.checkStrict(program))
    }

    @Test
    fun `string declaration matches the initializer`() {
        val program = program(variable("s", "string", stringExpr("hola")))

        assertTrue(checker.check(program).isOk)
    }

    @Test
    fun `mismatch between annotation and initializer`() {
        val program = program(variable("x", "number", stringExpr("hola")))
        val error = singleError(program)

        assertTrue(error.message.contains("Se esperaba number pero se encontró string"))
        assertEquals(location, error.location)
    }

    @Test
    fun `redeclaration of the same identifier`() {
        val program =
            program(
                variable("x", "number", numberExpr("1")),
                variable("x", "string", stringExpr("a")),
            )
        val error = singleError(program)

        assertTrue(error.message.contains("La variable 'x' ya fue declarada"))
    }

    @Test
    fun `unknown declared type`() {
        val program = program(variable("x", "boolean", numberExpr("1")))
        val error = singleError(program)

        assertTrue(error.message.contains("Tipo desconocido 'boolean'"))
    }

    @Test
    fun `undeclared variable in the initializer`() {
        val program = program(variable("x", "number", idExpr("y")))
        val error = singleError(program)

        assertTrue(error.message.contains("Variable 'y' no declarada"))
    }

    @Test
    fun `let x number equals x uses the scope before declaring`() {
        val program = program(variable("x", "number", idExpr("x")))
        val error = singleError(program)

        assertTrue(error.message.contains("Variable 'x' no declarada"))
    }

    @Test
    fun `a later declaration can reference a previous one`() {
        val program =
            program(
                variable("x", "number", numberExpr("1")),
                variable("y", "number", idExpr("x")),
            )

        assertTrue(checker.check(program).isOk)
    }

    @Test
    fun `expression statement validates the expression`() {
        val program = program(exprStmt(wrap("expression", call(numberExpr("1")))))

        assertTrue(checker.check(program).isOk)
    }

    @Test
    fun `expression statement reports undeclared identifiers`() {
        val program = program(exprStmt(idExpr("z")))
        val error = singleError(program)

        assertTrue(error.message.contains("Variable 'z' no declarada"))
    }

    @Test
    fun `unrecognized statement name`() {
        val program = program(leaf("mystery", "X", "x"))
        val error = singleError(program)

        assertTrue(error.message.contains("Nodo no reconocido"))
        assertTrue(error.message.contains("mystery"))
    }

    @Test
    fun `declaration kind does not depend on the node name`() {
        val local =
            TypeSystemConfig(
                types = listOf("number"),
                literals = mapOf("NUMBER_LITERAL" to "number"),
                nodes =
                    mapOf(
                        "let" to
                            NodeConfig(
                                kind = "declaration",
                                id = "name",
                                declaredType = "ty",
                                expression = "init",
                            ),
                        "number" to NodeConfig(kind = "literal"),
                        "init" to NodeConfig(kind = "literal"),
                    ),
            )
        val stmt =
            SyntaxNode(
                name = "let",
                children =
                    listOf(
                        leaf("name", "ID", "x"),
                        leaf("ty", "TYPE", "number"),
                        leaf("init", "NUMBER_LITERAL", "1"),
                    ),
                location = location,
            )

        assertTrue(DefaultTypeCheckerFactory.create(local).check(program(stmt)).isOk)
    }

    @Test
    fun `checkStrict returns the first error`() {
        val program = program(variable("x", "number", stringExpr("hola")))
        val result = checker.checkStrict(program)
        val error = assertIs<Result.Err<TypeError>>(result).error

        assertTrue(error.message.contains("Se esperaba number"))
    }

    @Test
    fun `check accumulates every error in the program`() {
        val program =
            program(
                variable("x", "number", stringExpr("hola")),
                variable("y", "boolean", numberExpr("1")),
                variable("z", "number", idExpr("missing")),
            )

        val report = checker.check(program)

        assertFalse(report.isOk)
        assertEquals(3, report.errors.size)
        assertTrue(report.errors[0].message.contains("Se esperaba number pero se encontró string"))
        assertTrue(report.errors[1].message.contains("Tipo desconocido 'boolean'"))
        assertTrue(report.errors[2].message.contains("Variable 'missing' no declarada"))
    }

    @Test
    fun `checkStrict stops at the first error and ignores the rest`() {
        val program =
            program(
                variable("x", "number", stringExpr("hola")),
                variable("y", "boolean", numberExpr("1")),
                variable("z", "number", idExpr("missing")),
            )

        val result = checker.checkStrict(program)
        val error = assertIs<Result.Err<TypeError>>(result).error

        assertTrue(error.message.contains("Se esperaba number pero se encontró string"))
        assertEquals(3, checker.check(program).errors.size)
    }

    private fun singleError(program: SyntaxProgram): TypeError {
        val report = checker.check(program)
        assertFalse(report.isOk)
        assertEquals(program, report.value)
        return report.errors.single()
    }

    private fun program(vararg statements: SyntaxNode): SyntaxProgram =
        SyntaxProgram(statements.toList(), statements.first().location)

    private fun variable(
        name: String,
        type: String,
        expression: SyntaxNode,
    ): SyntaxNode =
        SyntaxNode(
            name = "variable",
            children =
                listOf(
                    leaf("ID", "ID", name),
                    leaf("TYPE", "TYPE", type),
                    expression,
                ),
            location = location,
        )

    private fun exprStmt(expression: SyntaxNode): SyntaxNode =
        SyntaxNode(
            name = "expression-stmt",
            children = listOf(expression),
            location = expression.location,
        )

    private fun numberExpr(value: String): SyntaxNode = wrap("expression", leaf("number", "NUMBER_LITERAL", value))

    private fun stringExpr(value: String): SyntaxNode = wrap("expression", leaf("string", "STRING_LITERAL", value))

    private fun idExpr(name: String): SyntaxNode = wrap("expression", leaf("identifier", "ID", name))

    private fun call(argument: SyntaxNode): SyntaxNode =
        SyntaxNode(
            name = "call",
            children =
                listOf(
                    leaf("CALL", "CALL", "println"),
                    argument,
                ),
            location = location,
        )

    private fun wrap(
        name: String,
        child: SyntaxNode,
    ): SyntaxNode = SyntaxNode(name = name, children = listOf(child), location = child.location)

    private fun leaf(
        name: String,
        tokenType: String,
        value: String,
    ): SyntaxNode =
        SyntaxNode(
            name = name,
            token = Token(tokenType, Optional.of(value), location),
            location = location,
        )

    private fun canonicalConfig(): TypeSystemConfig =
        TypeSystemConfig(
            types = listOf("number", "string"),
            literals =
                mapOf(
                    "NUMBER_LITERAL" to "number",
                    "STRING_LITERAL" to "string",
                ),
            operations =
                listOf(
                    Operation("+", listOf("number", "number"), "number"),
                    Operation("+", listOf("string", "string"), "string"),
                    Operation("+", listOf("string", "number"), "string"),
                    Operation("-", listOf("number", "number"), "number"),
                    Operation("*", listOf("number", "number"), "number"),
                    Operation("/", listOf("number", "number"), "number"),
                ),
            nodes =
                mapOf(
                    "variable" to
                        NodeConfig(
                            kind = "declaration",
                            id = "ID",
                            declaredType = "TYPE",
                            expression = "expression",
                        ),
                    "expression-stmt" to
                        NodeConfig(
                            kind = "expression",
                            expression = "expression",
                        ),
                    "expression" to NodeConfig(kind = "binary-or-primary"),
                    "term" to NodeConfig(kind = "binary-or-primary"),
                    "factor" to NodeConfig(kind = "primary"),
                    "call" to
                        NodeConfig(
                            kind = "call",
                            callee = "CALL",
                            args = listOf("expression"),
                        ),
                    "group" to NodeConfig(kind = "group", expression = "expression"),
                    "number" to NodeConfig(kind = "literal"),
                    "string" to NodeConfig(kind = "literal"),
                    "identifier" to NodeConfig(kind = "identifier"),
                ),
        )
}
