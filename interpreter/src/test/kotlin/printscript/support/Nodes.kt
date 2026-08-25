package printscript.support

import java.util.Optional
import printscript.ast.Location
import printscript.domain.Token
import printscript.reader.CharPosition
import printscript.syntax.SyntaxNode

val TEST_LOCATION: Location = Location(CharPosition(1, 1), CharPosition(1, 1))

fun testToken(
    type: String,
    value: String? = null,
): Token = Token(type, Optional.ofNullable(value), TEST_LOCATION)

fun leaf(
    name: String,
    tokenType: String,
    value: String,
): SyntaxNode = SyntaxNode(name, token = testToken(tokenType, value), children = emptyList(), location = TEST_LOCATION)

fun node(
    name: String,
    vararg children: SyntaxNode,
): SyntaxNode = SyntaxNode(name, children = children.toList(), location = TEST_LOCATION)

fun numberNode(text: String): SyntaxNode = leaf("number", "NUMBER_LITERAL", text)

fun stringNode(content: String): SyntaxNode =
    SyntaxNode(
        name = "string",
        token = testToken("STRING_LITERAL", "\"$content\""),
        children = emptyList(),
        location = TEST_LOCATION,
    )

fun identifierNode(name: String): SyntaxNode = leaf("identifier", "ID", name)

fun operatorNode(symbol: String): SyntaxNode = leaf("OPERATOR", "OPERATOR", symbol)

fun binary(
    left: SyntaxNode,
    operator: String,
    right: SyntaxNode,
    name: String = "expression",
): SyntaxNode = node(name, left, operatorNode(operator), right)

fun call(
    argument: SyntaxNode,
    callee: String = "println",
): SyntaxNode = node("call", leaf("CALL", "CALL", callee), argument)
