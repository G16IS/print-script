package printscript.tools

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PrintScriptToolsExecuteTest {
    private fun run(
        source: String,
        inputs: List<String> = emptyList(),
        env: Map<String, String> = emptyMap(),
        version: String = "1.1",
    ) = PrintScriptTools.execute(version, source, inputs, env) as ExecutionOutcome.Finished

    @Test
    fun `prints in order`() {
        val out = run("println(\"a\");\nprintln(\"b\");")

        assertEquals(listOf("a", "b"), out.outputs)
        assertNull(out.error)
    }

    @Test
    fun `readInput consumes inputs in order and prints its prompt first`() {
        val out = run("let n: string = readInput(\"Name:\");\nprintln(n);", inputs = listOf("Ana", "Bob"))

        assertEquals(listOf("Name:", "Ana"), out.outputs)
        assertNull(out.error)
    }

    @Test
    fun `exhausted inputs fail and keep earlier outputs`() {
        val out = run("println(\"first\");\nlet n: string = readInput(\"Name:\");")

        assertEquals(listOf("first", "Name:"), out.outputs)
        assertNotNull(out.error)
    }

    @Test
    fun `readEnv reads only the given env`() {
        val out = run("println(readEnv(\"K\"));", env = mapOf("K" to "v"))

        assertEquals(listOf("v"), out.outputs)
        assertNull(out.error)
    }

    @Test
    fun `readEnv does not see the real environment`() {
        val out = run("println(readEnv(\"PATH\"));")

        assertNotNull(out.error)
        assertTrue(out.outputs.isEmpty())
    }

    @Test
    fun `runtime error keeps earlier outputs and reports a location`() {
        val out = run("println(\"ok\");\nprintln(missing);")

        assertEquals(listOf("ok"), out.outputs)
        assertTrue(out.error!!.contains("("))
    }

    @Test
    fun `unknown version is unsupported`() {
        assertEquals(
            ExecutionOutcome.UnsupportedVersion("9.9"),
            PrintScriptTools.execute("9.9", "println(1);", emptyList(), emptyMap()),
        )
    }

    @Test
    fun `version 1_0 runs`() {
        val out = run("println(\"hi\");", version = "1.0")

        assertEquals(listOf("hi"), out.outputs)
        assertNull(out.error)
    }
}
