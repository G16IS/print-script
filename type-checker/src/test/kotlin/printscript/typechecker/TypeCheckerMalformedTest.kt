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
import printscript.typechecker.handlers.CallHandler
import printscript.util.Result

class TypeCheckerMalformedTest {
    private val location = Location(CharPosition(1, 1), CharPosition(1, 2))
    private val config = canonicalConfig()
    private val factoryChecker = DefaultTypeCheckerFactory.create(config, DefaultKindHandlerFactory())
    private val resolver = DefaultExpressionTypeResolver(DefaultKindHandlerFactory())

    @Test
    fun `incomplete declaration reports an error`() {
        val stmt =
            SyntaxNode(
                name = "variable",
                children = listOf(leaf("ID", "ID", "x")),
                location = location,
            )
        val error = singleError(program(stmt))

        assertTrue(error.message.contains("Declaración incompleta"))
    }

    @Test
    fun `declaration without id mapping is incomplete`() {
        val local =
            config.copy(
                nodes =
                    config.nodes + (
                        "variable" to NodeConfig(kind = "declaration", declaredType = "TYPE", expression = "expression")
                    ),
            )
        val stmt = variable("x", "number", numberExpr("1"))
        val report = DefaultTypeCheckerFactory.create(local, DefaultKindHandlerFactory()).check(program(stmt))

        assertFalse(report.isOk)
        assertTrue(
            report.errors
                .single()
                .message
                .contains("Declaración incompleta"),
        )
    }

    @Test
    fun `declaration identifier without a token value is incomplete`() {
        val stmt =
            SyntaxNode(
                name = "variable",
                children =
                    listOf(
                        SyntaxNode(name = "ID", token = Token("ID", Optional.empty(), location), location = location),
                        leaf("TYPE", "TYPE", "number"),
                        numberExpr("1"),
                    ),
                location = location,
            )
        val error = singleError(program(stmt))

        assertTrue(error.message.contains("Declaración incompleta"))
    }

    @Test
    fun `declaration type without a token is incomplete`() {
        val stmt =
            SyntaxNode(
                name = "variable",
                children =
                    listOf(
                        leaf("ID", "ID", "x"),
                        SyntaxNode(name = "TYPE", location = location),
                        numberExpr("1"),
                    ),
                location = location,
            )
        val error = singleError(program(stmt))

        assertTrue(error.message.contains("Declaración incompleta"))
    }

    @Test
    fun `expression statement without the configured child is invalid`() {
        val stmt = SyntaxNode(name = "expression-stmt", location = location)
        val error = singleError(program(stmt))

        assertTrue(error.message.contains("Statement de expresión inválido"))
    }

    @Test
    fun `expression statement without expression mapping is invalid`() {
        val local =
            config.copy(
                nodes = config.nodes + ("expression-stmt" to NodeConfig(kind = "expression")),
            )
        val stmt = SyntaxNode(name = "expression-stmt", children = listOf(numberExpr("1")), location = location)
        val report = DefaultTypeCheckerFactory.create(local, DefaultKindHandlerFactory()).check(program(stmt))

        assertFalse(report.isOk)
        assertTrue(
            report.errors
                .single()
                .message
                .contains("Statement de expresión inválido"),
        )
    }

    @Test
    fun `unknown statement kind is reported`() {
        val local =
            config.copy(
                nodes = config.nodes + ("weird" to NodeConfig(kind = "nope")),
            )
        val stmt = SyntaxNode(name = "weird", location = location)
        val report = DefaultTypeCheckerFactory.create(local, DefaultKindHandlerFactory()).check(program(stmt))

        assertTrue(
            report.errors
                .single()
                .message
                .contains("Kind 'nope' no soportado"),
        )
    }

    @Test
    fun `explicit empty statement handlers still report unsupported kinds`() {
        val checker = DefaultTypeChecker(config, resolver, emptyList())
        val report = checker.check(program(variable("x", "number", numberExpr("1"))))

        assertTrue(
            report.errors
                .single()
                .message
                .contains("Kind 'declaration' no soportado"),
        )
    }

    @Test
    fun `checkStrict reports unrecognized statement names`() {
        val result = factoryChecker.checkStrict(program(SyntaxNode(name = "mystery", location = location)))

        assertIs<Result.Err<TypeError>>(result)
        assertTrue(result.error.message.contains("Nodo no reconocido"))
    }

    @Test
    fun `unknown expression kind is reported`() {
        val local =
            config.copy(
                nodes = config.nodes + ("weird-expr" to NodeConfig(kind = "lambda")),
            )
        val result = resolver.resolve(SyntaxNode(name = "weird-expr", location = location), ScopeStack(), local)
        val error = assertIs<Result.Err<TypeError>>(result).error

        assertTrue(error.message.contains("Kind 'lambda' no soportado"))
    }

    @Test
    fun `primary with no children is invalid`() {
        val result = resolver.resolve(SyntaxNode(name = "factor", location = location), ScopeStack(), config)
        val error = assertIs<Result.Err<TypeError>>(result).error

        assertTrue(error.message.contains("Primario inválido"))
    }

    @Test
    fun `binary-or-primary with no children is invalid`() {
        val result = resolver.resolve(SyntaxNode(name = "expression", location = location), ScopeStack(), config)
        val error = assertIs<Result.Err<TypeError>>(result).error

        assertTrue(error.message.contains("Expresión primaria inválida"))
    }

    @Test
    fun `binary operator without a value is invalid`() {
        val tree =
            SyntaxNode(
                name = "expression",
                children =
                    listOf(
                        wrap("term", leaf("number", "NUMBER_LITERAL", "1")),
                        SyntaxNode(name = "OPERATOR", location = location),
                        wrap("term", leaf("number", "NUMBER_LITERAL", "2")),
                    ),
                location = location,
            )
        val error = assertIs<Result.Err<TypeError>>(resolver.resolve(tree, ScopeStack(), config)).error

        assertTrue(error.message.contains("Operador inválido"))
    }

    @Test
    fun `binary reports an error on the right operand`() {
        val tree =
            SyntaxNode(
                name = "expression",
                children =
                    listOf(
                        wrap("term", leaf("number", "NUMBER_LITERAL", "1")),
                        leaf("OPERATOR", "OPERATOR", "+"),
                        wrap("term", leaf("identifier", "ID", "missing")),
                    ),
                location = location,
            )
        val error = assertIs<Result.Err<TypeError>>(resolver.resolve(tree, ScopeStack(), config)).error

        assertTrue(error.message.contains("Variable 'missing' no declarada"))
    }

    @Test
    fun `group without the configured child is invalid`() {
        val result = resolver.resolve(SyntaxNode(name = "group", location = location), ScopeStack(), config)
        val error = assertIs<Result.Err<TypeError>>(result).error

        assertTrue(error.message.contains("El grupo no tiene la expresión agrupada"))
    }

    @Test
    fun `group without expression mapping is invalid`() {
        val local = config.copy(nodes = config.nodes + ("group" to NodeConfig(kind = "group")))
        val result =
            resolver.resolve(
                SyntaxNode(name = "group", children = listOf(numberExpr("1")), location = location),
                ScopeStack(),
                local,
            )
        val error = assertIs<Result.Err<TypeError>>(result).error

        assertTrue(error.message.contains("El grupo no tiene la expresión agrupada"))
    }

    @Test
    fun `identifier without a token value is invalid`() {
        val node = SyntaxNode(name = "identifier", token = Token("ID", Optional.empty(), location), location = location)
        val error = assertIs<Result.Err<TypeError>>(resolver.resolve(node, ScopeStack(), config)).error

        assertTrue(error.message.contains("El identificador no tiene nombre"))
    }

    @Test
    fun `literal without a token is invalid`() {
        val node = SyntaxNode(name = "number", location = location)
        val error = assertIs<Result.Err<TypeError>>(resolver.resolve(node, ScopeStack(), config)).error

        assertTrue(error.message.contains("El literal no tiene token"))
    }

    @Test
    fun `declaration without type mapping is incomplete`() {
        val local =
            config.copy(
                nodes =
                    config.nodes + (
                        "variable" to NodeConfig(kind = "declaration", id = "ID", expression = "expression")
                    ),
            )
        val report =
            DefaultTypeCheckerFactory
                .create(local, DefaultKindHandlerFactory())
                .check(program(variable("x", "number", numberExpr("1"))))

        assertTrue(
            report.errors
                .single()
                .message
                .contains("Declaración incompleta"),
        )
    }

    @Test
    fun `declaration without expression mapping is incomplete`() {
        val local =
            config.copy(
                nodes =
                    config.nodes + (
                        "variable" to NodeConfig(kind = "declaration", id = "ID", declaredType = "TYPE")
                    ),
            )
        val report =
            DefaultTypeCheckerFactory
                .create(local, DefaultKindHandlerFactory())
                .check(program(variable("x", "number", numberExpr("1"))))

        assertTrue(
            report.errors
                .single()
                .message
                .contains("Declaración incompleta"),
        )
    }

    @Test
    fun `declaration identifier without a token object is incomplete`() {
        val stmt =
            SyntaxNode(
                name = "variable",
                children =
                    listOf(
                        SyntaxNode(name = "ID", location = location),
                        leaf("TYPE", "TYPE", "number"),
                        numberExpr("1"),
                    ),
                location = location,
            )

        assertTrue(singleError(program(stmt)).message.contains("Declaración incompleta"))
    }

    @Test
    fun `declaration type with an empty token value is incomplete`() {
        val stmt =
            SyntaxNode(
                name = "variable",
                children =
                    listOf(
                        leaf("ID", "ID", "x"),
                        SyntaxNode(
                            name = "TYPE",
                            token = Token("TYPE", Optional.empty(), location),
                            location = location,
                        ),
                        numberExpr("1"),
                    ),
                location = location,
            )

        assertTrue(singleError(program(stmt)).message.contains("Declaración incompleta"))
    }

    @Test
    fun `identifier without a token object is invalid`() {
        val node = SyntaxNode(name = "identifier", location = location)
        val error = assertIs<Result.Err<TypeError>>(resolver.resolve(node, ScopeStack(), config)).error

        assertTrue(error.message.contains("El identificador no tiene nombre"))
    }

    @Test
    fun `binary operator with an empty token value is invalid`() {
        val tree =
            SyntaxNode(
                name = "expression",
                children =
                    listOf(
                        wrap("term", leaf("number", "NUMBER_LITERAL", "1")),
                        SyntaxNode(
                            name = "OPERATOR",
                            token = Token("OPERATOR", Optional.empty(), location),
                            location = location,
                        ),
                        wrap("term", leaf("number", "NUMBER_LITERAL", "2")),
                    ),
                location = location,
            )
        val error = assertIs<Result.Err<TypeError>>(resolver.resolve(tree, ScopeStack(), config)).error

        assertTrue(error.message.contains("Operador inválido"))
    }

    @Test
    fun `call handler without a node config has no arguments to check`() {
        val handler = CallHandler(resolver)
        val node = SyntaxNode(name = "not-a-call", location = location)

        assertEquals(Result.Ok(""), handler.resolve(node, ScopeStack(), config))
    }

    @Test
    fun `call without args mapping still succeeds`() {
        val local = config.copy(nodes = config.nodes + ("call" to NodeConfig(kind = "call")))
        val call =
            SyntaxNode(
                name = "call",
                children = listOf(leaf("CALL", "CALL", "println"), numberExpr("1")),
                location = location,
            )

        assertEquals(Result.Ok(""), resolver.resolve(call, ScopeStack(), local))
    }

    private fun singleError(program: SyntaxProgram): printscript.typechecker.TypeError {
        val report = factoryChecker.check(program)
        assertFalse(report.isOk)
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
            children = listOf(leaf("ID", "ID", name), leaf("TYPE", "TYPE", type), expression),
            location = location,
        )

    private fun numberExpr(value: String): SyntaxNode = wrap("expression", leaf("number", "NUMBER_LITERAL", value))

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
            literals = mapOf("NUMBER_LITERAL" to "number", "STRING_LITERAL" to "string"),
            operations = listOf(Operation("+", listOf("number", "number"), "number")),
            nodes =
                mapOf(
                    "variable" to
                        NodeConfig(
                            kind = "declaration",
                            id = "ID",
                            declaredType = "TYPE",
                            expression = "expression",
                        ),
                    "expression-stmt" to NodeConfig(kind = "expression", expression = "expression"),
                    "expression" to NodeConfig(kind = "binary-or-primary"),
                    "term" to NodeConfig(kind = "binary-or-primary"),
                    "factor" to NodeConfig(kind = "primary"),
                    "call" to NodeConfig(kind = "call", callee = "CALL", args = listOf("expression")),
                    "group" to NodeConfig(kind = "group", expression = "expression"),
                    "number" to NodeConfig(kind = "literal"),
                    "string" to NodeConfig(kind = "literal"),
                    "identifier" to NodeConfig(kind = "identifier"),
                ),
        )
}
