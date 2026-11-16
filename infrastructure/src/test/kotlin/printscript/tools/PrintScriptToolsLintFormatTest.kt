package printscript.tools

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.validation.IssueCodes

class PrintScriptToolsLintFormatTest {
    private val camelCase =
        """{"rules": {"identifier-format": {"enabled": true, "options": {"format": "camelCase"}}}}"""

    private fun lintIssues(
        source: String,
        rules: String,
    ) = (PrintScriptTools.lint("1.1", source, rules) as LintOutcome.Checked).issues

    private fun formatted(rules: String) =
        (PrintScriptTools.format("1.1", "let x:number=1;", rules) as FormatOutcome.Formatted).code

    @Test
    fun `lint of clean code is clean`() {
        val outcome = PrintScriptTools.lint("1.1", "let myVar: number = 1;", camelCase)

        assertTrue((outcome as LintOutcome.Checked).isClean)
    }

    @Test
    fun `lint reports a rule violation with its location`() {
        val issues = lintIssues("let my_var: number = 1;", camelCase)

        assertEquals(1, issues.size)
        assertEquals(IssueCodes.LINT, issues[0].code)
        assertEquals(1, issues[0].line)
        assertEquals(5, issues[0].column)
    }

    @Test
    fun `lint with empty rules reports nothing`() {
        assertEquals(emptyList<Any>(), lintIssues("let my_var: number = 1;", "{}"))
    }

    @Test
    fun `lint of code that does not parse reports a syntax issue`() {
        val issues = lintIssues("let x: number = ;", camelCase)

        assertEquals(IssueCodes.SYNTAX, issues.first().code)
    }

    @Test
    fun `lint with an unknown rule id is invalid rules`() {
        val outcome = PrintScriptTools.lint("1.1", "let x: number = 1;", """{"rules": {"nope": {"enabled": true}}}""")

        assertTrue(outcome is LintOutcome.InvalidRules)
    }

    @Test
    fun `lint with malformed json is invalid rules`() {
        assertTrue(PrintScriptTools.lint("1.1", "let x: number = 1;", """{"rules":""") is LintOutcome.InvalidRules)
    }

    @Test
    fun `lint with an unknown version is unsupported`() {
        assertEquals(LintOutcome.UnsupportedVersion("9.9"), PrintScriptTools.lint("9.9", "", camelCase))
    }

    @Test
    fun `format applies the rules sent by the caller`() {
        val on = formatted("""{"rules": [{"type": "space-before-colon", "enabled": true}]}""")
        val off = formatted("""{"rules": [{"type": "space-before-colon", "enabled": false}]}""")

        assertEquals("let x : number = 1;\n", on)
        assertEquals("let x: number = 1;\n", off)
    }

    @Test
    fun `format of code that does not parse is rejected with a syntax issue`() {
        val outcome = PrintScriptTools.format("1.1", "let x: number = ;", """{"rules": []}""")

        assertEquals(IssueCodes.SYNTAX, (outcome as FormatOutcome.Rejected).issues.first().code)
    }

    @Test
    fun `format with an unknown rule type is invalid rules`() {
        val outcome =
            PrintScriptTools.format(
                "1.1",
                "let x: number = 1;",
                """{"rules": [{"type": "nope", "enabled": true}]}""",
            )

        assertTrue(outcome is FormatOutcome.InvalidRules)
    }

    @Test
    fun `format with malformed json is invalid rules`() {
        assertTrue(PrintScriptTools.format("1.1", "let x: number = 1;", """{"rules":""") is FormatOutcome.InvalidRules)
    }

    @Test
    fun `format with an unknown version is unsupported`() {
        assertEquals(FormatOutcome.UnsupportedVersion("9.9"), PrintScriptTools.format("9.9", "", """{"rules": []}"""))
    }
}
