package printscript

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.error.UndeclaredIdentifier
import printscript.util.Result

class InterpreterContextTest {
    @Test
    fun `declared variable is visible`() {
        val context = InterpreterContext().declareVariable("x", NumberValue(1.0))

        assertEquals(NumberValue(1.0), context.getVariable("x"))
    }

    @Test
    fun `unknown variable resolves to null`() {
        assertNull(InterpreterContext().getVariable("nope"))
    }

    @Test
    fun `child scope sees parent variables`() {
        val parent = InterpreterContext().declareVariable("x", StringValue("hola"))

        assertEquals(StringValue("hola"), parent.childScope().getVariable("x"))
    }

    @Test
    fun `declaring in child scope shadows parent`() {
        val child = InterpreterContext().declareVariable("x", NumberValue(1.0)).childScope()
        val shadowed = child.declareVariable("x", NumberValue(2.0))

        assertEquals(NumberValue(2.0), shadowed.getVariable("x"))
    }

    @Test
    fun `assign rebuilds the chain without mutating previous contexts`() {
        val root = InterpreterContext().declareVariable("x", NumberValue(1.0))
        val child = root.childScope()

        val assigned = child.assignVariable("x", NumberValue(9.0))

        assertTrue(assigned is Result.Ok)
        val newChild = (assigned as Result.Ok).value
        assertEquals(NumberValue(9.0), newChild.getVariable("x"))
        assertEquals(NumberValue(1.0), child.getVariable("x"))
        assertEquals(NumberValue(1.0), root.getVariable("x"))
    }

    @Test
    fun `assigning an undeclared variable fails`() {
        val result = InterpreterContext().assignVariable("y", NumberValue(1.0))

        assertTrue(result is Result.Err)
        assertTrue((result as Result.Err).error is UndeclaredIdentifier)
    }
}
