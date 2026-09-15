package printscript.tck

import java.io.ByteArrayInputStream
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import printscript.config.PrintScriptConfigs
import printscript.domain.FormatRuleSpec
import printscript.domain.FormatterRulesConfig
import printscript.edition.LanguageCatalog
import printscript.io.DefaultSideEffectManager
import printscript.reader.FileCodeReader
import printscript.usecases.FormatCode
import printscript.util.Result

class PrintScriptConfigsLoaderTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `load with empty user still applies colon spaces from user-defaults`() {
        val configs = PrintScriptConfigsLoader.load("1.0", FormatterRulesConfig())
        val formatted = format(configs, "let x:number=1;")

        assertEquals("let x : number = 1;\n", formatted)
    }

    @Test
    fun `a partial user config wins over user-defaults rule by rule`() {
        val user =
            FormatterRulesConfig(
                listOf(FormatRuleSpec(type = "space-around-assign", enabled = false)),
            )
        val configs = PrintScriptConfigsLoader.load("1.0", user)
        val formatted = format(configs, "let x:number=1;")

        assertTrue(formatted.contains("number=1"))
        assertFalse(formatted.contains("number = 1"))
        assertTrue(formatted.contains("x : number"), "las rules no mandadas siguen tomando el default interno")
    }

    @Test
    fun `PrintScript format lets a complete user config control spacing`() {
        val config =
            """
            {
              "rules": [
                { "type": "space-before-colon", "enabled": false },
                { "type": "space-after-colon", "enabled": false },
                { "type": "space-around-assign", "enabled": false },
                { "type": "newlines-before-println", "count": 0 }
              ]
            }
            """.trimIndent()
        val source = tempDir.resolve("sample.ps")
        Files.writeString(source, "let x:number=1;")
        val writer = StringWriter()

        PrintScript.format(
            "1.0",
            FileCodeReader(source.toAbsolutePath().toString()),
            ByteArrayInputStream(config.toByteArray(StandardCharsets.UTF_8)),
            writer,
        )

        assertEquals("let x:number=1;\n", writer.toString())
    }

    private fun format(
        configs: PrintScriptConfigs,
        source: String,
    ): String {
        val path = tempDir.resolve("sample.ps")
        Files.writeString(path, source)
        val kit =
            (LanguageCatalog.of("1.0", DefaultSideEffectManager()) as Result.Ok).value
        val writer = StringWriter()
        FormatCode.formatForTck(
            configs,
            FileCodeReader(path.toAbsolutePath().toString()),
            writer,
            kit,
        )
        return writer.toString()
    }
}
