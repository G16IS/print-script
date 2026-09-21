package printscript.typechecker

import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import printscript.domain.NodeConfig
import printscript.domain.Token
import printscript.domain.TypeSystemConfig
import printscript.error.TypeError
import printscript.reader.CharPosition
import printscript.syntax.Location
import printscript.syntax.SyntaxNode
import printscript.syntax.SyntaxProgram
import printscript.typechecker.support.typeSystem
import printscript.util.Result
import printscript.util.isOk

class MalformedTreeTest {
    private val location = Location(CharPosition(1, 1), CharPosition(1, 2))
    private val checker = checker()

    @Test
    fun `unrecognized statement name`() {
        val error = errorOf(program(node("mystery")))
        assertTrue(error.message.contains("Nodo no reconocido: 'mystery'"))
    }

    @Test
    fun `unknown statement kind`() {
        val bare = DefaultTypeChecker(typeSystem(), resolver(), emptyList())
        val error = errorOf(program(node("variable")), bare)
        assertTrue(error.message.contains("Kind 'declaration' no soportado"))
    }

    @Test
    fun `incomplete declaration`() {
        val error = errorOf(program(node("variable")))
        assertTrue(error.message.contains("Declaración incompleta"))
    }

    @Test
    fun `literal without a token`() {
        val error = resolve(node("number"))
        assertTrue(error.message.contains("El literal no tiene token"))
    }

    @Test
    fun `identifier without a name`() {
        val error = resolve(node("identifier", token = Token("ID", Optional.empty(), location)))
        assertTrue(error.message.contains("El identificador no tiene nombre"))
    }

    @Test
    fun `primary with no children`() {
        val error = resolve(node("factor"))
        assertTrue(error.message.contains("Primario inválido"))
    }

    @Test
    fun `group without the grouped expression`() {
        val error = resolve(node("group"))
        assertTrue(error.message.contains("El grupo no tiene la expresión agrupada"))
    }

    @Test
    fun `expression statement without the configured child`() {
        val error = errorOf(program(node("expression-stmt")))
        assertTrue(error.message.contains("Statement de expresión inválido"))
    }

    @Test
    fun `unknown expression kind`() {
        val config =
            TypeSystemConfig(
                types = listOf("number"),
                nodes = mapOf("mystery" to NodeConfig(kind = "nope")),
            )
        val result = ExpressionTypeResolver(DefaultKindHandlerFactory()).resolve(node("mystery"), ScopeStack(), config)
        val error = assertIs<Result.Err<TypeError>>(result).error
        assertTrue(error.message.contains("Kind 'nope' no soportado"))
    }

    @Test
    fun `call with no configured argument child succeeds`() {
        val call =
            node(
                "call",
                leaf("CALL", "CALL", "println"),
                leaf("number", "NUMBER_LITERAL", "1"),
            )
        val result = resolver().resolve(call, ScopeStack(), typeSystem())
        assertTrue(result.isOk)
        assertEquals("", (result as Result.Ok).value)
    }

    private fun errorOf(
        program: SyntaxProgram,
        typeChecker: TypeChecker = checker,
    ): TypeError {
        val result = typeChecker.check(program)
        return assertIs<Result.Err<TypeError>>(result).error
    }

    private fun resolve(node: SyntaxNode): TypeError {
        val result = resolver().resolve(node, ScopeStack(), typeSystem())
        return assertIs<Result.Err<TypeError>>(result).error
    }

    private fun resolver() = ExpressionTypeResolver(DefaultKindHandlerFactory())

    private fun checker() = TypeChecker.create(typeSystem(), DefaultKindHandlerFactory())

    private fun program(statement: SyntaxNode) = SyntaxProgram(listOf(statement), statement.location)

    private fun node(
        name: String,
        vararg children: SyntaxNode,
        token: Token? = null,
    ) = SyntaxNode(name, token, children.toList(), location)

    private fun leaf(
        name: String,
        tokenType: String,
        value: String,
    ) = node(name, token = Token(tokenType, Optional.of(value), location))
}
