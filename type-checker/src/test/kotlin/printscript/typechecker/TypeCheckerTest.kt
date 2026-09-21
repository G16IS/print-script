package printscript.typechecker

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import printscript.typechecker.support.assertErr
import printscript.typechecker.support.assertOk
import printscript.typechecker.support.assertWalk
import printscript.typechecker.support.checker
import printscript.typechecker.support.parse
import printscript.typechecker.support.typeSystem
import printscript.typechecker.support.walk
import printscript.util.Result

class TypeCheckerTest {
    @Nested
    inner class Declarations {
        @Test
        fun `number declaration matches the initializer`() = assertOk("let x: number = 1;")

        @Test
        fun `string declaration matches the initializer`() = assertOk("let s: string = \"hola\";")

        @Test
        fun `declaration without initializer still declares the annotated type`() =
            assertOk("let x: number;\nprintln(x);")

        @Test
        fun `a later declaration can reference a previous one`() = assertOk("let x: number = 1;\nlet y: number = x;")

        @Test
        fun `mismatch between annotation and initializer`() =
            assertErr("let x: number = \"hola\";", "Se esperaba number pero se encontró string")

        @Test
        fun `redeclaration of the same identifier`() =
            assertErr("let x: number = 1;\nlet x: string = \"a\";", "La variable 'x' ya fue declarada")

        @Test
        fun `let x number equals x uses the scope before declaring`() =
            assertErr("let x: number = x;", "Variable 'x' no declarada")

        @Test
        fun `undeclared variable in the initializer`() = assertErr("let x: number = y;", "Variable 'y' no declarada")

        @Test
        fun `a failed declaration still binds the name`() {
            val walked = walk("let x: number = \"hola\";\nprintln(x);")
            assertEquals(listOf("Se esperaba number pero se encontró string"), walked.errors.map { it.message })
            assertEquals("number", walked.scope.lookup("x"))
        }

        @Test
        fun `readEnv adapts to the declared type`() = assertOk("let x: number = readEnv(\"A\");")

        @Test
        fun `const cannot be reassigned`() =
            assertErr("const x: number = 1;\nx = 2;", "No se puede asignar a la constante 'x'")

        @Test
        fun `assignment updates a mutable variable`() = assertOk("let x: number = 1;\nx = 2;")
    }

    @Nested
    inner class Expressions {
        @Test
        fun `expression statement validates the expression`() = assertOk("println(1 + 2 * 3);")

        @Test
        fun `expression statement reports undeclared identifiers`() =
            assertErr("println(missing);", "Variable 'missing' no declarada")

        @Test
        fun `string plus number is string`() = assertOk("let x: string = \"a\" + 1;")

        @Test
        fun `number plus string matches via permutation`() = assertOk("let x: string = 1 + \"a\";")

        @Test
        fun `string minus number is rejected`() =
            assertErr("let x: number = \"a\" - 1;", "El operador '-' no acepta string y number")

        @Test
        fun `grouped addition is number`() = assertOk("let x: number = (1 + 2) * 3;")

        @Test
        fun `plus is not swapped when the operation is not commutative`() {
            val base = typeSystem()
            val config =
                base.copy(
                    operations =
                        base.operations.map { op ->
                            if (op.op == "+") op.copy(commutative = false) else op
                        },
                )
            val result = checker(config).check(parse("println(1 + \"a\");"))
            assertTrue(result is Result.Err)
            assertTrue((result as Result.Err).error.message.contains("no acepta number y string"))
        }
    }

    @Nested
    inner class Control {
        @Test
        fun `if accepts a boolean condition`() = assertOk("if (true) { println(1); }")

        @Test
        fun `if rejects a number condition`() =
            assertErr("if (1) { println(1); }", "Se esperaba boolean pero se encontró number")

        @Test
        fun `a binding inside the if does not escape`() =
            assertErr("if (true) { let x: number = 1; }\nprintln(x);", "Variable 'x' no declarada")

        @Test
        fun `check returns the first error and stops`() =
            assertErr(
                "let x: number = \"hola\";\nprintln(missing);",
                "Se esperaba number pero se encontró string",
            )

        @Test
        fun `walk keeps later statement errors`() =
            assertWalk(
                "let x: number = \"hola\";\nprintln(missing);",
                "Se esperaba number pero se encontró string",
                "Variable 'missing' no declarada",
            )
    }
}
