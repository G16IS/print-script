package printscript.typechecker

import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import printscript.domain.NodeConfig
import printscript.domain.Token
import printscript.domain.TypeSystemConfig
import printscript.reader.CharPosition
import printscript.syntax.Location
import printscript.syntax.SyntaxNode
import printscript.util.Result

class ExpressionTypeResolverTest {
    private val location = Location(CharPosition(2, 5), CharPosition(2, 8))
    private val resolver = DefaultExpressionTypeResolver()
    private val config =
        TypeSystemConfig(
            types = listOf("number", "string"),
            literals =
                mapOf(
                    "NUMBER_LITERAL" to "number",
                    "STRING_LITERAL" to "string",
                    "INT_LIT" to "number",
                ),
            nodes =
                mapOf(
                    "number" to NodeConfig(kind = "literal"),
                    "string" to NodeConfig(kind = "literal"),
                    "int" to NodeConfig(kind = "literal"),
                    "identifier" to NodeConfig(kind = "identifier"),
                    "ref" to NodeConfig(kind = "identifier"),
                ),
        )

    @Test
    fun `number literal resolves to number via literals map`() {
        val result = resolver.resolve(leaf("number", "NUMBER_LITERAL", "42"), ScopeStack(), config)

        assertEquals(Result.Ok("number"), result)
    }

    @Test
    fun `string literal resolves to string via literals map`() {
        val result = resolver.resolve(leaf("string", "STRING_LITERAL", "hola"), ScopeStack(), config)

        assertEquals(Result.Ok("string"), result)
    }

    @Test
    fun `literal uses config kind and token type not the node name`() {
        val result = resolver.resolve(leaf("int", "INT_LIT", "7"), ScopeStack(), config)

        assertEquals(Result.Ok("number"), result)
    }

    @Test
    fun `identifier resolves to the type in scope`() {
        val scope = ScopeStack().declare("x", "number")

        val result = resolver.resolve(leaf("identifier", "ID", "x"), scope!!, config)

        assertEquals(Result.Ok("number"), result)
    }

    @Test
    fun `identifier uses config kind not the node name`() {
        val scope = ScopeStack().declare("pepe", "string")

        val result = resolver.resolve(leaf("ref", "ID", "pepe"), scope!!, config)

        assertEquals(Result.Ok("string"), result)
    }

    @Test
    fun `undeclared identifier returns TypeError with location`() {
        val result = resolver.resolve(leaf("identifier", "ID", "x"), ScopeStack(), config)
        val error = errorOf(result)

        assertTrue(error.message.contains("Variable 'x' no declarada"))
        assertEquals(location, error.location)
    }

    @Test
    fun `unknown node name returns TypeError`() {
        val result = resolver.resolve(leaf("boolean", "TRUE", "true"), ScopeStack(), config)
        val error = errorOf(result)

        assertTrue(error.message.contains("Nodo no reconocido"))
        assertTrue(error.message.contains("boolean"))
        assertEquals(location, error.location)
    }

    @Test
    fun `literal token type missing from config returns TypeError`() {
        val result = resolver.resolve(leaf("number", "TRUE", "true"), ScopeStack(), config)
        val error = errorOf(result)

        assertTrue(error.message.contains("TRUE"))
        assertEquals(location, error.location)
    }

    private fun errorOf(result: Result<String, TypeError>): TypeError {
        val err = assertIs<Result.Err<TypeError>>(result)
        return err.error
    }

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
}
