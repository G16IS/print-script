package printscript.tck

import java.io.ByteArrayInputStream
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.util.Optional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.domain.FormatterRulesConfig
import printscript.edition.LanguageCatalog
import printscript.io.DefaultSideEffectManager
import printscript.reader.CharPosition
import printscript.reader.CodeReader
import printscript.usecases.FormatCode
import printscript.util.Result

class PrintScriptFormatTckTest {
    private val fixture =
        """
        let something:string="a really cool thing";
        println(something);
        println("x");
        """.trimIndent()

    @Test
    fun `space-around-assign enabled false omits spaces around equals`() {
        val output =
            formatWith(
                rulesJson(
                    aroundAssign = false,
                ),
            )

        assertTrue(output.contains("string=\""))
        assertFalse(output.contains("string ="))
    }

    @Test
    fun `space-around-assign enabled true inserts spaces around equals`() {
        val output =
            formatWith(
                rulesJson(
                    aroundAssign = true,
                ),
            )

        assertTrue(output.contains("string = \""))
    }

    @Test
    fun `space-before-colon enabled true inserts space before colon`() {
        val output =
            formatWith(
                rulesJson(
                    beforeColon = true,
                ),
            )

        assertTrue(output.contains("something :"))
    }

    @Test
    fun `space-after-colon enabled true inserts space after colon when before is off`() {
        val output =
            formatWith(
                rulesJson(
                    afterColon = true,
                ),
            )

        assertTrue(output.contains(": string"))
        assertFalse(output.contains("something :"))
    }

    @Test
    fun `newlines-before-println count 0 keeps a single newline between printlns`() {
        val output =
            formatWith(
                rulesJson(
                    newlinesBeforePrintln = 0,
                ),
            )
        val between =
            output
                .substringAfter("println(something);")
                .substringBefore("println(\"x\")")

        assertEquals("\n", between)
    }

    @Test
    fun `newlines-before-println count 2 inserts two blank lines between printlns`() {
        val output =
            formatWith(
                rulesJson(
                    newlinesBeforePrintln = 2,
                ),
            )
        val between =
            output
                .substringAfter("println(something);")
                .substringBefore("println(\"x\")")

        assertEquals("\n\n\n", between)
    }

    @Test
    fun `CLI load with empty user still has colon spaces from defaults`() {
        val configs = PrintScriptConfigsLoader.load("1.0", FormatterRulesConfig())
        val kit =
            (LanguageCatalog.of("1.0", DefaultSideEffectManager()) as Result.Ok).value
        val writer = StringWriter()

        FormatCode.formatForTck(
            configs,
            StringCodeReader("let x:number=1;"),
            writer,
            kit,
        )

        assertTrue(writer.toString().contains("x : number"))
    }

    private fun formatWith(configJson: String): String {
        val writer = StringWriter()
        PrintScript.format(
            "1.0",
            StringCodeReader(fixture),
            ByteArrayInputStream(configJson.toByteArray(StandardCharsets.UTF_8)),
            writer,
        )
        return writer.toString()
    }

    private fun rulesJson(
        beforeColon: Boolean = false,
        afterColon: Boolean = false,
        aroundAssign: Boolean = false,
        newlinesBeforePrintln: Int = 0,
    ): String =
        """
        {
          "rules": [
            { "type": "space-before-colon", "enabled": $beforeColon },
            { "type": "space-after-colon", "enabled": $afterColon },
            { "type": "space-around-assign", "enabled": $aroundAssign },
            { "type": "newlines-before-println", "count": $newlinesBeforePrintln }
          ]
        }
        """.trimIndent()

    /** In-memory [CodeReader]; positions match [FileCodeReader] (1-based). */
    private class StringCodeReader(
        source: String,
    ) : CodeReader {
        private val realReader = source.reader().buffered()
        private var currentPosition = CharPosition(1, 1)
        private var lookahead = realReader.read()

        override fun read(): Optional<Char> {
            if (lookahead == -1) return Optional.empty()

            val char = lookahead.toChar()
            currentPosition =
                if (char == '\n') {
                    CharPosition(currentPosition.line + 1, 1)
                } else {
                    CharPosition(currentPosition.line, currentPosition.col + 1)
                }

            lookahead = realReader.read()
            return Optional.of(char)
        }

        override fun peek(): Optional<Char> {
            if (lookahead == -1) return Optional.empty()
            return Optional.of(lookahead.toChar())
        }

        override fun currentPosition(): CharPosition = currentPosition
    }
}
