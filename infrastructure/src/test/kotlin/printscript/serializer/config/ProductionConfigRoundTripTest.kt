package printscript.serializer.config

import kotlinx.serialization.KSerializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ProductionConfigRoundTripTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("configs")
    fun `production config resource round-trips`(case: RoundTripCase) {
        case.run()
    }

    companion object {
        @JvmStatic
        fun configs() =
            listOf(
                roundTripCase("language v1.0", LanguageConfigSerializer, "language.config.v1.0.json"),
                roundTripCase("language v1.1", LanguageConfigSerializer, "language.config.v1.1.json"),
                roundTripCase("grammar v1.0", GrammarSerializer, "grammar.config.v1.0.json"),
                roundTripCase("grammar v1.1", GrammarSerializer, "grammar.config.v1.1.json"),
                roundTripCase(
                    "type-system v1.0",
                    TypeSystemConfigSerializer,
                    "type-system.config.v1.0.json",
                ),
                roundTripCase(
                    "type-system v1.1",
                    TypeSystemConfigSerializer,
                    "type-system.config.v1.1.json",
                ),
                roundTripCase(
                    "formatter-language v1.0",
                    FormatterLanguageConfigSerializer,
                    "formatter-language.v1.0.json",
                ),
                roundTripCase(
                    "formatter-language v1.1",
                    FormatterLanguageConfigSerializer,
                    "formatter-language.v1.1.json",
                ),
                roundTripCase(
                    "formatter-user-defaults",
                    FormatterRulesConfigSerializer,
                    "formatter-user-defaults.json",
                ),
                roundTripCase("linter v1.0", LinterConfigSerializer, "linter.config.v1.0.json"),
                roundTripCase("linter v1.1", LinterConfigSerializer, "linter.config.v1.1.json"),
            )

        private fun <T> roundTripCase(
            name: String,
            serializer: KSerializer<T>,
            resource: String,
        ) = RoundTripCase(name) {
            val original = decode(serializer, resourceText(resource))
            val encoded = serializerJson.encodeToString(serializer, original)
            val again = decode(serializer, encoded)
            assertEquals(original, again)
        }
    }
}

class RoundTripCase(
    val name: String,
    val run: () -> Unit,
) {
    override fun toString(): String = name
}
