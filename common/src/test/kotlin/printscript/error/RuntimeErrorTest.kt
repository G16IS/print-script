package printscript.error

import kotlin.test.Test
import kotlin.test.assertEquals
import printscript.support.loc

class RuntimeErrorTest {
    private val location = loc(1, 2, 1, 8)

    @Test
    fun `UnresolvableExpression names the node`() {
        val error = UnresolvableExpression(nodeName = "if", location = location)

        assertEquals("No hay handler registrado para nodo 'if'", error.message)
        assertEquals(location, error.location)
    }

    @Test
    fun `DivisionByZero has a fixed Spanish message`() {
        assertEquals("División por cero", DivisionByZero(location).message)
    }

    @Test
    fun `InvalidLiteral names the lexeme`() {
        assertEquals("Literal inválido '1.'", InvalidLiteral("1.", location).message)
    }

    @Test
    fun `UnresolvableCall names the callee`() {
        assertEquals("Llamada desconocida 'foo'", UnresolvableCall("foo", location).message)
    }
}
