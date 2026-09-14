package printscript.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FormatterLanguageConfigTest {
    @Test
    fun `accepts empty config`() {
        val config = FormatterLanguageConfig()

        assertEquals(emptyList(), config.rules)
        assertEquals(emptyList(), config.userBindings)
    }

    @Test
    fun `rejects duplicate userType`() {
        val binding =
            UserRuleBinding(
                userType = "space-before-colon",
                type = "space-before",
                token = "COLON",
                param = UserRuleBinding.PARAM_ENABLED,
                defaultEnabled = true,
            )

        val error =
            assertFailsWith<IllegalArgumentException> {
                FormatterLanguageConfig(userBindings = listOf(binding, binding))
            }

        assertEquals(
            true,
            error.message!!.contains("Duplicate userType"),
        )
    }

    @Test
    fun `rejects unknown binding param`() {
        assertFailsWith<IllegalArgumentException> {
            FormatterLanguageConfig(
                userBindings =
                    listOf(
                        UserRuleBinding(
                            userType = "space-before-colon",
                            type = "space-before",
                            token = "COLON",
                            param = "indent",
                        ),
                    ),
            )
        }
    }

    @Test
    fun `rejects a language rule with a blank token`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                FormatterLanguageConfig(
                    rules = listOf(LanguageFormatRuleSpec(type = "space-before", token = "")),
                )
            }

        assertTrue(error.message!!.contains("needs a token"))
    }

    @Test
    fun `accepts language rules with a non-blank token`() {
        val config =
            FormatterLanguageConfig(
                rules =
                    listOf(
                        LanguageFormatRuleSpec(
                            type = "space-before",
                            token = "COLON",
                            value = " ",
                            previous = "ID",
                        ),
                    ),
            )

        assertEquals("COLON", config.rules.single().token)
        assertEquals(" ", config.rules.single().value)
    }

    @Test
    fun `rejects a blank userType`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                FormatterLanguageConfig(
                    userBindings =
                        listOf(
                            UserRuleBinding(
                                userType = "  ",
                                type = "space-before",
                                token = "COLON",
                                param = UserRuleBinding.PARAM_ENABLED,
                            ),
                        ),
                )
            }

        assertTrue(error.message!!.contains("userType must not be blank"))
    }

    @Test
    fun `rejects a binding with a blank token`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                FormatterLanguageConfig(
                    userBindings =
                        listOf(
                            UserRuleBinding(
                                userType = "space-before-colon",
                                type = "space-before",
                                token = "",
                                param = UserRuleBinding.PARAM_ENABLED,
                            ),
                        ),
                )
            }

        assertTrue(error.message!!.contains("needs a token"))
    }

    @Test
    fun `rejects enabled binding that defines defaultCount`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                FormatterLanguageConfig(
                    userBindings =
                        listOf(
                            UserRuleBinding(
                                userType = "space-before-colon",
                                type = "space-before",
                                token = "COLON",
                                param = UserRuleBinding.PARAM_ENABLED,
                                defaultCount = 1,
                            ),
                        ),
                )
            }

        assertTrue(error.message!!.contains("must not define defaultCount"))
    }

    @Test
    fun `rejects count binding that defines defaultEnabled`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                FormatterLanguageConfig(
                    userBindings =
                        listOf(
                            UserRuleBinding(
                                userType = "indent",
                                type = "indent",
                                token = "LET",
                                param = UserRuleBinding.PARAM_COUNT,
                                defaultEnabled = true,
                            ),
                        ),
                )
            }

        assertTrue(error.message!!.contains("must not define defaultEnabled"))
    }

    @Test
    fun `accepts an enabled binding without defaultCount`() {
        val binding =
            UserRuleBinding(
                userType = "space-before-colon",
                type = "space-before",
                token = "COLON",
                param = UserRuleBinding.PARAM_ENABLED,
                defaultEnabled = true,
            )

        val config = FormatterLanguageConfig(userBindings = listOf(binding))

        assertEquals(listOf(binding), config.userBindings)
    }

    @Test
    fun `accepts a count binding without defaultEnabled`() {
        val binding =
            UserRuleBinding(
                userType = "line-jump-after-semicolon",
                type = "line-jump-after",
                token = "SEMICOLON",
                param = UserRuleBinding.PARAM_COUNT,
                defaultCount = 1,
            )

        val config = FormatterLanguageConfig(userBindings = listOf(binding))

        assertEquals(1, config.userBindings.single().defaultCount)
    }
}
