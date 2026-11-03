package printscript.reader

import java.io.Reader
import java.io.StringReader
import java.util.Optional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class ReaderCodeReaderTest {
    private fun reader(text: String) = ReaderCodeReader(StringReader(text))

    private fun ReaderCodeReader.readAll(): String {
        val out = StringBuilder()
        while (peek().isPresent) out.append(read().get())
        return out.toString()
    }

    @Test
    fun `starts at line 1 column 1`() {
        assertEquals(CharPosition(1, 1), reader("abc").currentPosition())
    }

    @Test
    fun `reads every character in order`() {
        assertEquals("let x;\nfoo", reader("let x;\nfoo").readAll())
    }

    @Test
    fun `each character moves the column one place`() {
        val reader = reader("abc")

        reader.read()
        assertEquals(CharPosition(1, 2), reader.currentPosition())
        reader.read()
        reader.read()
        assertEquals(CharPosition(1, 4), reader.currentPosition())
    }

    @Test
    fun `a line break goes to the next line and resets the column`() {
        val reader = reader("ab\ncd")

        repeat(3) { reader.read() }

        assertEquals(CharPosition(2, 1), reader.currentPosition())
    }

    @Test
    fun `a carriage return counts as a column and only the line feed breaks the line`() {
        val reader = reader("a\r\nb")

        reader.read()
        reader.read()
        assertEquals(CharPosition(1, 3), reader.currentPosition())
        reader.read()
        assertEquals(CharPosition(2, 1), reader.currentPosition())
    }

    @Test
    fun `peek shows the next character without consuming it`() {
        val reader = reader("xy")

        assertEquals(Optional.of('x'), reader.peek())
        assertEquals(Optional.of('x'), reader.peek())
        assertEquals(CharPosition(1, 1), reader.currentPosition())
        assertEquals(Optional.of('x'), reader.read())
        assertEquals(Optional.of('y'), reader.peek())
    }

    @Test
    fun `an empty source has nothing to read`() {
        val reader = reader("")

        assertEquals(Optional.empty<Char>(), reader.peek())
        assertEquals(Optional.empty<Char>(), reader.read())
        assertEquals(CharPosition(1, 1), reader.currentPosition())
    }

    @Test
    fun `reading past the end keeps returning empty and does not move the position`() {
        val reader = reader("a")
        reader.read()

        assertEquals(Optional.empty<Char>(), reader.read())
        assertEquals(Optional.empty<Char>(), reader.read())
        assertEquals(CharPosition(1, 2), reader.currentPosition())
    }

    @Test
    fun `it does not close the reader it was given`() {
        var closed = false
        val source =
            object : Reader() {
                private val inner = StringReader("abc")

                override fun read(
                    buffer: CharArray,
                    offset: Int,
                    length: Int,
                ): Int = inner.read(buffer, offset, length)

                override fun close() {
                    closed = true
                }
            }

        ReaderCodeReader(source).readAll()

        assertFalse(closed)
    }

    @Test
    fun `StringCodeReader keeps the same positions as before`() {
        val reader = StringCodeReader("ab\ncd")

        assertEquals(CharPosition(1, 1), reader.currentPosition())
        assertEquals("ab\ncd", buildString { while (reader.peek().isPresent) append(reader.read().get()) })
        assertEquals(CharPosition(2, 3), reader.currentPosition())
    }
}
