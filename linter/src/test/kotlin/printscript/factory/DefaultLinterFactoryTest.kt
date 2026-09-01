package printscript.factory

import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import printscript.ast.Location
import printscript.domain.LinterConfig
import printscript.domain.RuleConfig
import printscript.domain.Token
import printscript.reader.CharPosition
import printscript.syntax.SyntaxNode
import printscript.syntax.SyntaxProgram

class DefaultLinterFactoryTest {
    private val loc =
        Location(
            start = CharPosition(1, 1),
            end = CharPosition(1, 20),
        )

    private fun variableNode(identifier: String): SyntaxNode {
        val idNode =
            SyntaxNode(
                name = "ID",
                token = Token("ID", Optional.of(identifier), loc),
                location = loc,
            )
        return SyntaxNode(
            name = "variable",
            children = listOf(idNode),
            location = loc,
        )
    }

    @Test
    fun `creates linter and applies enabled rules`() {
        val config =
            LinterConfig(
                rules =
                    mapOf(
                        "identifier-format" to RuleConfig(enabled = true, options = mapOf("format" to "snake_case")),
                    ),
            )

        val linter = DefaultLinterFactory.create(config)
        assertNotNull(linter)

        // With snake_case enabled, camelCase should fail
        val report = linter.lint(SyntaxProgram(listOf(variableNode("camelCaseVar")), loc))
        assertFalse(report.isOk)
    }

    @Test
    fun `disabled rules are not executed`() {
        val config =
            LinterConfig(
                rules =
                    mapOf(
                        "identifier-format" to RuleConfig(enabled = false, options = mapOf("format" to "snake_case")),
                    ),
            )

        val linter = DefaultLinterFactory.create(config)

        // With rule disabled, camelCase should pass
        val report = linter.lint(SyntaxProgram(listOf(variableNode("camelCaseVar")), loc))
        assertTrue(report.isOk)
    }

    @Test
    fun `unknown rule id throws IllegalArgumentException on creation`() {
        val config =
            LinterConfig(
                rules =
                    mapOf(
                        "non-existent-rule" to RuleConfig(enabled = true),
                    ),
            )

        assertFailsWith<IllegalArgumentException> {
            DefaultLinterFactory.create(config)
        }
    }

    @Test
    fun `duplicate provider throws IllegalArgumentException on creation`() {
        val config = LinterConfig()
        val duplicateProviders =
            listOf(
                IdentifierFormatRuleProvider(),
                IdentifierFormatRuleProvider(),
            )

        assertFailsWith<IllegalArgumentException> {
            DefaultLinterFactory.create(config, duplicateProviders)
        }
    }

    @Test
    fun `invalid format option in IdentifierFormatRuleProvider throws IllegalArgumentException`() {
        val config =
            LinterConfig(
                rules =
                    mapOf(
                        "identifier-format" to RuleConfig(enabled = true, options = mapOf("format" to "PascalCase")),
                    ),
            )

        assertFailsWith<IllegalArgumentException> {
            DefaultLinterFactory.create(config)
        }
    }
}
