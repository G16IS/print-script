package printscript.tck

import java.io.ByteArrayInputStream
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.domain.FormatterRulesConfig
import printscript.edition.LanguageCatalog
import printscript.io.DefaultSideEffectManager
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
    fun `single-space-separation puts exactly one space between every pair of tokens`() {
        val config =
            """
            {
              "rules": [
                { "type": "single-space-separation", "enabled": true },
                { "type": "space-before-colon", "enabled": false },
                { "type": "space-after-colon", "enabled": false },
                { "type": "space-around-assign", "enabled": false },
                { "type": "newlines-before-println", "count": 0 }
              ]
            }
            """.trimIndent()
        val writer = StringWriter()

        PrintScript.format(
            "1.0",
            StringCodeReader("let something:      string=\"a really cool thing\";\nprintln(something);"),
            ByteArrayInputStream(config.toByteArray(StandardCharsets.UTF_8)),
            writer,
        )

        // golden de printscript-tck/formatter/1.0/enforce-single-space-separation
        assertEquals(
            "let something : string = \"a really cool thing\" ;\nprintln ( something ) ;\n",
            writer.toString(),
        )
    }

    @Test
    fun `single-space-separation stays off unless the config turns it on`() {
        val writer = StringWriter()

        PrintScript.format(
            "1.0",
            StringCodeReader("let something:      string=\"a really cool thing\";\nprintln(something);"),
            ByteArrayInputStream(rulesJson().toByteArray(StandardCharsets.UTF_8)),
            writer,
        )

        assertEquals("let something:string=\"a really cool thing\";\nprintln(something);\n", writer.toString())
    }

    @Test
    fun `non configurable rules hold whatever the TCK config says`() {
        val messy = "let    x   :    string    =    \"a\"      +    \"b\"   ;\nprintln   (   x   )   ;"
        val writer = StringWriter()

        PrintScript.format(
            "1.0",
            StringCodeReader(messy),
            ByteArrayInputStream(
                rulesJson(newlinesBeforePrintln = 0).toByteArray(StandardCharsets.UTF_8),
            ),
            writer,
        )

        // un espacio como máximo entre tokens, espacio alrededor del operador,
        // salto de línea después del `;` — ninguna de las tres se configura.
        assertEquals("let x:string=\"a\" + \"b\";\nprintln(x);\n", writer.toString())
    }

    @Test
    fun `an empty TCK config falls back to the internal defaults`() {
        val writer = StringWriter()

        PrintScript.format(
            "1.0",
            StringCodeReader("let x:number=1;"),
            ByteArrayInputStream("{}".toByteArray(StandardCharsets.UTF_8)),
            writer,
        )

        assertEquals("let x : number = 1;\n", writer.toString())
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
}
