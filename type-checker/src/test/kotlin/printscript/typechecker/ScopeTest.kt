package printscript.typechecker

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull

class ScopeTest {
    @Test
    fun `declare returns a new Scope and leaves the original unchanged`() {
        val original = Scope()

        val declared = original.declare("x", "number")

        assertNotNull(declared)
        assertNotSame(original, declared)
        assertNull(original.lookup("x"))
        assertEquals("number", declared.lookup("x"))
    }

    @Test
    fun `lookup returns null for an unknown name`() {
        assertNull(Scope().lookup("x"))
    }

    @Test
    fun `redeclaration in the same Scope returns null`() {
        val withX = Scope().declare("x", "number")

        assertNotNull(withX)
        assertNull(withX.declare("x", "string"))
        assertEquals("number", withX.lookup("x"))
    }

    @Test
    fun `declare of a different name keeps previous symbols`() {
        val withX = Scope().declare("x", "number")
        val withBoth = withX?.declare("y", "string")

        assertNotNull(withBoth)
        assertEquals("number", withBoth.lookup("x"))
        assertEquals("string", withBoth.lookup("y"))
        assertNull(withX.lookup("y"))
    }
}
