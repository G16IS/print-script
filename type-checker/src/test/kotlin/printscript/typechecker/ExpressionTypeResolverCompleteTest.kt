package printscript.typechecker

import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import printscript.application.factory.typechecker.V1KindHandlerFactory
import printscript.domain.NodeConfig
import printscript.domain.Operation
import printscript.domain.Token
import printscript.domain.TypeSystemConfig
import printscript.reader.CharPosition
import printscript.syntax.Location
import printscript.syntax.SyntaxNode
import printscript.util.Result
import printscript.util.isOk

class ExpressionTypeResolverCompleteTest {
    private val location = Location(CharPosition(1, 1), CharPosition(1, 2))
    private val resolver = DefaultExpressionTypeResolver(V1KindHandlerFactory())
    private val config = canonicalConfig()

    @Test
    fun `1 plus 2 times 3 is number`() {
        val tree =
            add(
                termOf(num("1")),
                mul(num("2"), num("3")),
            )

        assertEquals(Result.Ok("number"), resolver.resolve(tree, ScopeStack(), config))
    }

    @Test
    fun `string plus number is string`() {
        val tree = add(termOf(str("a")), termOf(num("1")))

        assertEquals(Result.Ok("string"), resolver.resolve(tree, ScopeStack(), config))
    }

    @Test
    fun `number plus string matches via permutation`() {
        val tree = add(termOf(num("1")), termOf(str("a")))

        assertEquals(Result.Ok("string"), resolver.resolve(tree, ScopeStack(), config))
    }

    @Test
    fun `string minus number is rejected`() {
        val tree = binary("expression", termOf(str("a")), "-", termOf(num("1")))
        val error = errorOf(resolver.resolve(tree, ScopeStack(), config))

        assertTrue(error.message.contains("El operador '-' no acepta string y number"))
        assertEquals(location, error.location)
    }

    @Test
    fun `variable plus number uses the declared type`() {
        val scope = ScopeStack().declare("x", "number")
        val tree = add(termOf(id("x")), termOf(num("1")))

        assertEquals(Result.Ok("number"), resolver.resolve(tree, scope!!, config))
    }

    @Test
    fun `undeclared variable in a binary expression fails`() {
        val tree = add(termOf(id("x")), termOf(num("1")))
        val error = errorOf(resolver.resolve(tree, ScopeStack(), config))

        assertTrue(error.message.contains("Variable 'x' no declarada"))
    }

    @Test
    fun `grouped 1 plus 2 times 3 is number`() {
        val inner = add(termOf(num("1")), termOf(num("2")))
        val tree = exprOf(mul(group(inner), num("3")))

        assertEquals(Result.Ok("number"), resolver.resolve(tree, ScopeStack(), config))
    }

    @Test
    fun `call resolves each argument`() {
        val tree = call(add(termOf(num("1")), termOf(num("2"))))

        assertTrue(resolver.resolve(tree, ScopeStack(), config).isOk)
    }

    @Test
    fun `call fails when an argument is undeclared`() {
        val tree = call(exprOf(termOf(id("x"))))
        val error = errorOf(resolver.resolve(tree, ScopeStack(), config))

        assertTrue(error.message.contains("Variable 'x' no declarada"))
    }

    @Test
    fun `primary wraps a single child`() {
        val tree = wrap("factor", num("4"))

        assertEquals(Result.Ok("number"), resolver.resolve(tree, ScopeStack(), config))
    }

    @Test
    fun `binary kind does not depend on the node name`() {
        val tree = binary("sum", termOf(num("1")), "+", termOf(num("2")))
        val local =
            TypeSystemConfig(
                types = listOf("number"),
                literals = mapOf("NUMBER_LITERAL" to "number"),
                operations =
                    listOf(
                        Operation("+", listOf("number", "number"), "number"),
                    ),
                nodes =
                    mapOf(
                        "number" to NodeConfig(kind = "literal"),
                        "term" to NodeConfig(kind = "binary-or-primary"),
                        "sum" to NodeConfig(kind = "binary-or-primary"),
                    ),
            )

        assertEquals(Result.Ok("number"), resolver.resolve(tree, ScopeStack(), local))
    }

    @Test
    fun `non commutative operation is not permuted`() {
        val local =
            TypeSystemConfig(
                types = listOf("number", "string"),
                literals =
                    mapOf(
                        "NUMBER_LITERAL" to "number",
                        "STRING_LITERAL" to "string",
                    ),
                operations =
                    listOf(
                        Operation("-", listOf("number", "string"), "number", commutative = false),
                    ),
                nodes =
                    mapOf(
                        "number" to NodeConfig(kind = "literal"),
                        "string" to NodeConfig(kind = "literal"),
                        "term" to NodeConfig(kind = "binary-or-primary"),
                        "expression" to NodeConfig(kind = "binary-or-primary"),
                    ),
            )
        val tree = binary("expression", termOf(str("a")), "-", termOf(num("1")))
        val error = errorOf(resolver.resolve(tree, ScopeStack(), local))

        assertTrue(error.message.contains("El operador '-' no acepta string y number"))
    }

    private fun errorOf(result: Result<String, TypeError>): TypeError {
        val err = assertIs<Result.Err<TypeError>>(result)
        return err.error
    }

    private fun num(value: String): SyntaxNode = leaf("number", "NUMBER_LITERAL", value)

    private fun str(value: String): SyntaxNode = leaf("string", "STRING_LITERAL", value)

    private fun id(name: String): SyntaxNode = leaf("identifier", "ID", name)

    private fun termOf(factor: SyntaxNode): SyntaxNode = wrap("term", factor)

    private fun exprOf(term: SyntaxNode): SyntaxNode = wrap("expression", term)

    private fun add(
        left: SyntaxNode,
        right: SyntaxNode,
    ): SyntaxNode = binary("expression", left, "+", right)

    private fun mul(
        left: SyntaxNode,
        right: SyntaxNode,
    ): SyntaxNode = binary("term", left, "*", right)

    private fun group(expression: SyntaxNode): SyntaxNode = wrap("group", expression)

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

    private fun binary(
        name: String,
        left: SyntaxNode,
        op: String,
        right: SyntaxNode,
    ): SyntaxNode =
        SyntaxNode(
            name = name,
            children =
                listOf(
                    left,
                    leaf("OPERATOR", "OPERATOR", op),
                    right,
                ),
            location = location,
        )

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
