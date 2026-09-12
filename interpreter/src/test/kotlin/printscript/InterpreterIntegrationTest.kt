package printscript

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.error.DivisionByZero
import printscript.error.UndeclaredIdentifier
import printscript.support.PsSupport
import printscript.support.createInterpreter
import printscript.support.ok
import printscript.util.Result

class InterpreterIntegrationTest {
    @Test
    fun `runs the sample program end to end`() {
        val program =
            PsSupport.parse(
                """
                let pepe: string = "Hello, World!";
                let pepa: number = 42;
                println(pepe);
                println(1 + 2 * 3);
                """.trimIndent(),
            )

        val result = createInterpreter("1").interpret(InterpreterContext(), program)

        assertEquals(
            listOf(PrintEffect("Hello, World!"), PrintEffect("7")),
            ok(result),
        )
    }

    @Test
    fun `prints whole numbers without decimals and decimals as-is`() {
        val program =
            PsSupport.parse(
                """
                let a: number = 1.5;
                let b: number = 4 / 2;
                println(a);
                println(b);
                """.trimIndent(),
            )
        val effects = ok(createInterpreter("1").interpret(InterpreterContext(), program))

        assertEquals(listOf(PrintEffect("1.5"), PrintEffect("2")), effects)
    }

    @Test
    fun `concatenates strings with plus`() {
        val program =
            PsSupport.parse(
                """
                let a: string = "hola";
                let b: string = "mundo";
                println(a + b);
                """.trimIndent(),
            )
        val effects = ok(createInterpreter("1").interpret(InterpreterContext(), program))

        assertEquals(listOf(PrintEffect("holamundo")), effects)
    }

    @Test
    fun `grouping overrides precedence`() {
        val program = PsSupport.parse("println((1 + 2) * 3);")
        val effects = ok(createInterpreter("1").interpret(InterpreterContext(), program))

        assertEquals(listOf(PrintEffect("9")), effects)
    }

    @Test
    fun `undeclared variable at runtime fails with UndeclaredIdentifier`() {
        val program = PsSupport.parse("println(nope);")

        val result = createInterpreter("1").interpret(InterpreterContext(), program)

        assertTrue(result is Result.Err)
        assertTrue((result as Result.Err).error is UndeclaredIdentifier)
    }

    @Test
    fun `division by zero at runtime fails with DivisionByZero`() {
        val program = PsSupport.parse("println(1 / 0);")

        val result = createInterpreter("1").interpret(InterpreterContext(), program)

        assertTrue(result is Result.Err)
        assertTrue((result as Result.Err).error is DivisionByZero)
    }
}
