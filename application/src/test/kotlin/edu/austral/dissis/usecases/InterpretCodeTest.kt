package edu.austral.dissis.usecases

import edu.austral.dissis.testing.ParseExample
import edu.austral.dissis.testing.ast.AstBuilder
import edu.austral.dissis.testing.ast.assertAst
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InterpretCodeTest {
    @Test
    fun `declarations and prints produce the expected tree`() {
        val report = ParseExample.parse("declarations_and_prints.ps")
        assertTrue(report.isOk)
        assertAst(report.value!!) {
            node("variable") {
                node("ID", "pepe")
                node("TYPE", "string")
                node("expression") {
                    node("term") {
                        node("string", "\"Hello, World!\"")
                    }
                }
            }
            node("variable") {
                node("ID", "pepa")
                node("TYPE", "number")
                node("expression") {
                    node("term") {
                        node("number", "42")
                    }
                }
            }
            printCall("pepe")
            printCall("pepa")
        }
    }

    @Test
    fun `binary expression respects precedence`() {
        val report = ParseExample.parse("binary_expression.ps")
        assertTrue(report.isOk)
        assertAst(report.value!!) {
            node("variable") {
                node("ID", "x")
                node("TYPE", "number")
                node("expression") {
                    node("term") { node("number", "1") }
                    node("OPERATOR", "+")
                    node("term") {
                        node("number", "2")
                        node("OPERATOR", "*")
                        node("number", "3")
                    }
                }
            }
            printCall("x")
        }
    }

    @Test
    fun `string literal is kept as a token value`() {
        val report = ParseExample.parse("string_literal.ps")
        assertTrue(report.isOk)
        assertAst(report.value!!) {
            node("variable") {
                node("ID", "greeting")
                node("TYPE", "string")
                node("expression") {
                    node("term") {
                        node("string", "\"hola\"")
                    }
                }
            }
            printCall("greeting")
        }
    }

    @Test
    fun `type mismatch fails type check`() {
        val report = ParseExample.parse("type_mismatch.ps")

        assertFalse(report.isOk)
        assertTrue(report.errors.any { it.message.contains("Se esperaba number pero se encontró string") })
    }

    @Test
    fun `undeclared variable fails type check`() {
        val report = ParseExample.parse("undeclared_variable.ps")

        assertFalse(report.isOk)
        assertTrue(report.errors.any { it.message.contains("Variable 'x' no declarada") })
    }

    @Test
    fun `redeclaration fails type check`() {
        val report = ParseExample.parse("redeclaration.ps")

        assertFalse(report.isOk)
        assertTrue(report.errors.any { it.message.contains("La variable 'x' ya fue declarada") })
    }

    private fun AstBuilder.printCall(name: String) {
        node("expression-stmt") {
            node("expression") {
                node("term") {
                    node("call") {
                        node("CALL", "println")
                        node("expression") {
                            node("term") {
                                node("identifier", name)
                            }
                        }
                    }
                }
            }
        }
    }
}
