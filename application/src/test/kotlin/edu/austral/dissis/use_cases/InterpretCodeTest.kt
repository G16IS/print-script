package edu.austral.dissis.use_cases

import edu.austral.dissis.testing.ParseExample
import edu.austral.dissis.testing.ast.AstBuilder
import edu.austral.dissis.testing.ast.assertAst
import org.junit.jupiter.api.Test

class InterpretCodeTest {

    @Test
    fun `declarations and prints produce the expected tree`() {
        val program = ParseExample.parse("declarations_and_prints.ps")
        assertAst(program) {
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
        val program = ParseExample.parse("binary_expression.ps")
        assertAst(program) {
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
        val program = ParseExample.parse("string_literal.ps")
        assertAst(program) {
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
