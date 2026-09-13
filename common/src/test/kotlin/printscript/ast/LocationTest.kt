package printscript.ast

import kotlin.test.Test
import kotlin.test.assertEquals
import printscript.reader.CharPosition

class LocationTest {
    @Test
    fun `empty is origin to origin`() {
        val location = Location.empty()

        assertEquals(CharPosition(0, 0), location.start)
        assertEquals(CharPosition(0, 0), location.end)
    }
}
