package printscript.validation

import java.io.IOException
import java.io.InputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PrintScriptValidationStreamTest {
    private fun fromStream(
        version: String,
        source: String,
    ): ValidationOutcome = PrintScriptValidation.validate(version, source.byteInputStream())

    private fun issues(outcome: ValidationOutcome): List<ValidationIssue> =
        (outcome as ValidationOutcome.Checked).issues

    @Test
    fun `a stream gives the same outcome as the same text`() {
        val sources =
            listOf(
                "",
                "let x: number = 1;\nprintln(x);",
                "let x: number = \"hola\";",
                "let x: number = 1;\nprintln(y);",
                "let x number = 1;",
                "let @",
                "let a: number = 1;\n\n\nprintln(nope);",
                "let x: number = \"hola\";\nprintln(x);\nlet z: number = \"no\";",
            )

        sources.forEach { source ->
            listOf("1.0", "1.1").forEach { version ->
                assertEquals(
                    PrintScriptValidation.validate(version, source),
                    fromStream(version, source),
                    "version $version, source '$source'",
                )
            }
        }
    }

    @Test
    fun `a valid program from a stream has no issues`() {
        assertEquals(emptyList<ValidationIssue>(), issues(fromStream("1.0", "let x: number = 1;\nprintln(x);")))
    }

    @Test
    fun `an error in a stream is reported with its line and column`() {
        val found = issues(fromStream("1.0", "let x: number = 1;\nprintln(y);"))

        assertEquals(1, found.size)
        assertEquals(IssueCodes.TYPE, found[0].code)
        assertEquals(2, found[0].line)
        assertEquals(9, found[0].column)
    }

    @Test
    fun `columns count characters, not bytes`() {
        // ñ y ú ocupan 2 bytes cada una en UTF-8: contando bytes el @ estaría en la columna 21.
        val found = issues(fromStream("1.0", "println(\"ñandú\"); @"))

        assertEquals(IssueCodes.SYNTAX, found[0].code)
        assertEquals(1, found[0].line)
        assertEquals(19, found[0].column)
    }

    @Test
    fun `windows line endings still give the right line`() {
        val found = issues(fromStream("1.0", "let x: number = 1;\r\nprintln(y);"))

        assertEquals(2, found[0].line)
    }

    @Test
    fun `bytes that are not valid UTF-8 are replaced instead of failing`() {
        val bytes = "let x: string = \"".toByteArray() + byteArrayOf(0xFF.toByte()) + "\";".toByteArray()

        val outcome = PrintScriptValidation.validate("1.0", bytes.inputStream())

        assertEquals(emptyList<ValidationIssue>(), issues(outcome))
    }

    @Test
    fun `an unsupported version does not touch the stream`() {
        val untouchable =
            object : InputStream() {
                override fun read(): Int = throw IOException("the stream must not be read")
            }

        assertEquals(
            ValidationOutcome.UnsupportedVersion("9.9"),
            PrintScriptValidation.validate("9.9", untouchable),
        )
    }

    @Test
    fun `the stream stays open for whoever passed it`() {
        val source = GeneratedStream(head = "let x: number = 1;".toByteArray())

        PrintScriptValidation.validate("1.0", source)

        assertFalse(source.closed)
    }

    @Test
    fun `a syntax error stops the reading instead of consuming the rest`() {
        // Unos 12 TB de código válido después del error: si se cargara todo, el test no terminaría.
        val source =
            GeneratedStream(
                head = "println(1);\nprintln(1);\n@\n".toByteArray(),
                body = "println(1);\n".toByteArray(),
                bodyRepeats = HUGE,
            )

        val found = issues(PrintScriptValidation.validate("1.0", source))

        assertEquals(IssueCodes.SYNTAX, found[0].code)
        assertEquals(3, found[0].line)
        assertEquals(1, found[0].column)
        assertTrue(source.consumed < MAX_CONSUMED, "consumed ${source.consumed} bytes before stopping")
    }

    @Test
    fun `a long valid program is read to the end and a late error keeps its line`() {
        val lines = 200_000L
        val source =
            GeneratedStream(
                body = "println(1);\n".toByteArray(),
                bodyRepeats = lines,
                tail = "println(nope);".toByteArray(),
            )

        val found = issues(PrintScriptValidation.validate("1.0", source))

        assertEquals(1, found.size)
        assertEquals(IssueCodes.TYPE, found[0].code)
        assertEquals(lines + 1, found[0].line.toLong())
        assertEquals(source.size, source.consumed)
    }

    private companion object {
        const val HUGE = 1_000_000_000_000L
        const val MAX_CONSUMED = 1_000_000L
    }

    /** Genera `head`, luego `body` repetido `bodyRepeats` veces y luego `tail`, sin materializarlo. */
    private class GeneratedStream(
        private val head: ByteArray = ByteArray(0),
        private val body: ByteArray = ByteArray(1),
        private val bodyRepeats: Long = 0,
        private val tail: ByteArray = ByteArray(0),
    ) : InputStream() {
        private val bodySize = body.size * bodyRepeats
        val size: Long = head.size + bodySize + tail.size

        var consumed = 0L
            private set
        var closed = false
            private set

        override fun read(): Int {
            val one = ByteArray(1)
            return if (read(one, 0, 1) == -1) -1 else one[0].toInt() and 0xFF
        }

        override fun read(
            buffer: ByteArray,
            offset: Int,
            length: Int,
        ): Int {
            if (consumed >= size) return -1
            val count = minOf(length.toLong(), size - consumed).toInt()
            for (i in 0 until count) buffer[offset + i] = byteAt(consumed + i)
            consumed += count
            return count
        }

        private fun byteAt(position: Long): Byte =
            when {
                position < head.size -> head[position.toInt()]
                position < head.size + bodySize -> body[((position - head.size) % body.size).toInt()]
                else -> tail[(position - head.size - bodySize).toInt()]
            }

        override fun close() {
            closed = true
        }
    }
}
