package printscript.typechecker

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

class ScopeStackTest {
    @Test
    fun `declare returns a new ScopeStack and leaves the original unchanged`() {
        val original = ScopeStack()

        val declared = original.declare("x", "number")

        assertNotNull(declared)
        assertNotSame(original, declared)
        assertNull(original.lookup("x"))
        assertEquals("number", declared.lookup("x"))
    }

    @Test
    fun `lookup returns null for an unknown name`() {
        assertNull(ScopeStack().lookup("x"))
    }

    @Test
    fun `redeclaration in the current scope returns null`() {
        val withX = ScopeStack().declare("x", "number")

        assertNotNull(withX)
        assertNull(withX.declare("x", "string"))
        assertEquals("number", withX.lookup("x"))
    }

    @Test
    fun `push returns a new ScopeStack and leaves the original unchanged`() {
        val original = ScopeStack().declare("x", "number")

        assertNotNull(original)
        val pushed = original.push()

        assertNotSame(original, pushed)
        assertEquals("number", original.lookup("x"))
        assertEquals("number", pushed.lookup("x"))
    }

    @Test
    fun `pop on the root stack returns the same instance`() {
        val root = ScopeStack()

        assertSame(root, root.pop())
    }

    @Test
    fun `pop returns a new instance and drops inner declarations`() {
        val outer = ScopeStack().declare("x", "number")

        assertNotNull(outer)
        val inner = outer.push().declare("y", "string")

        assertNotNull(inner)
        assertEquals("string", inner.lookup("y"))

        val popped = inner.pop()

        assertNotSame(inner, popped)
        assertEquals("number", popped.lookup("x"))
        assertNull(popped.lookup("y"))
        assertEquals("string", inner.lookup("y"))
    }

    @Test
    fun `inner declare shadows outer and pop restores the outer type`() {
        val outer = ScopeStack().declare("x", "number")

        assertNotNull(outer)
        val inner = outer.push().declare("x", "string")

        assertNotNull(inner)
        assertEquals("string", inner.lookup("x"))
        assertEquals("number", inner.pop().lookup("x"))
        assertEquals("number", outer.lookup("x"))
    }

    @Test
    fun `lookup walks from inner scope to outer`() {
        val outer = ScopeStack().declare("x", "number")

        assertNotNull(outer)
        val inner = outer.push().declare("y", "string")

        assertNotNull(inner)
        assertEquals("number", inner.lookup("x"))
        assertEquals("string", inner.lookup("y"))
    }
}
