package printscript

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.definitions.PrintEffect
import printscript.error.DivisionByZero
import printscript.error.UndeclaredIdentifier
import printscript.error.UninitializedVariable
import printscript.support.PsSupport
import printscript.support.createInterpreter
import printscript.support.interpretEffects
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

        assertEquals(
            listOf(PrintEffect("Hello, World!"), PrintEffect("7")),
            interpretEffects("1.0", program),
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
        assertEquals(listOf(PrintEffect("1.5"), PrintEffect("2")), interpretEffects("1.0", program))
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
        assertEquals(listOf(PrintEffect("holamundo")), interpretEffects("1.0", program))
    }

    @Test
    fun `grouping overrides precedence`() {
        val program = PsSupport.parse("println((1 + 2) * 3);")
        assertEquals(listOf(PrintEffect("9")), interpretEffects("1.0", program))
    }

    @Test
    fun `undeclared variable at runtime fails with UndeclaredIdentifier`() {
        val program = PsSupport.parse("println(nope);")

        val result = createInterpreter("1.0").interpret(InterpreterContext(), program)

        assertTrue(result is Result.Err)
        assertTrue((result as Result.Err).error is UndeclaredIdentifier)
    }

    @Test
    fun `single quoted strings print without quotes`() {
        val program = PsSupport.parse("println('hola');")
        assertEquals(listOf(PrintEffect("hola")), interpretEffects("1.0", program))
    }

    @Test
    fun `reading an uninitialized variable fails`() {
        val program =
            PsSupport.parse(
                """
                let x: string;
                println(x);
                """.trimIndent(),
            )
        val result = createInterpreter("1.0").interpret(InterpreterContext(), program)
        assertTrue(result is Result.Err)
        assertTrue((result as Result.Err).error is UninitializedVariable)
    }

    @Test
    fun `uninitialized declaration by itself succeeds`() {
        val program = PsSupport.parse("let x: number;")
        val result = createInterpreter("1.0").interpret(InterpreterContext(), program)
        assertTrue(result is Result.Ok)
    }

    @Test
    fun `division by zero at runtime fails with DivisionByZero`() {
        val program = PsSupport.parse("println(1 / 0);")

        val result = createInterpreter("1.0").interpret(InterpreterContext(), program)

        assertTrue(result is Result.Err)
        assertTrue((result as Result.Err).error is DivisionByZero)
    }
}
