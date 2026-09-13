package printscript.reader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.domain.ExactRule
import printscript.domain.RegexRule

class JSONLanguageConfigReaderTest {
    @Test
    fun `reads the production language config`() {
        val config =
            JSONLanguageConfigReader.read(
                requireNotNull(javaClass.classLoader.getResourceAsStream("language.config.v1.0.json")),
            )

        assertEquals(
            listOf("keywords", "types", "operators", "literals", "identifiers"),
            config.order,
        )
        assertTrue(config.config["keywords"]!!.any { it is ExactRule && it.token == "LET" })
        assertTrue(config.config["literals"]!!.any { it is RegexRule && it.token == "STRING_LITERAL" })
    }

    @Test
    fun `1 dot 1 language config matches 1 dot 0`() {
        val v10 =
            JSONLanguageConfigReader.read(
                requireNotNull(javaClass.classLoader.getResourceAsStream("language.config.v1.0.json")),
            )
        val v11 =
            JSONLanguageConfigReader.read(
                requireNotNull(javaClass.classLoader.getResourceAsStream("language.config.v1.1.json")),
            )
        assertEquals(v10.order, v11.order)
        assertEquals(v10.config.keys, v11.config.keys)
    }
}
