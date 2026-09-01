package printscript.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
}
