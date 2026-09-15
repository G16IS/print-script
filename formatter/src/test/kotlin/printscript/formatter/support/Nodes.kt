package printscript.formatter.support

import java.util.Optional
import printscript.domain.Token
import printscript.reader.CharPosition
import printscript.syntax.Location
import printscript.syntax.SyntaxNode
import printscript.syntax.SyntaxProgram

fun token(
    type: String,
    value: String,
    startCol: Int,
    line: Int = 1,
): Token {
    val start = CharPosition(line, startCol)
    val end = CharPosition(line, startCol + value.length - 1)

    return Token(type, Optional.of(value), Location(start, end))
}

fun leaf(
    type: String,
    value: String,
    startCol: Int,
    line: Int = 1,
): SyntaxNode {
    val token = token(type, value, startCol, line)

    return SyntaxNode(name = type, token = token, location = token.location)
}

fun wrap(
    name: String,
    child: SyntaxNode,
): SyntaxNode = SyntaxNode(name = name, children = listOf(child), location = child.location)

fun initializer(expression: SyntaxNode? = null): SyntaxNode {
    if (expression == null) {
        return SyntaxNode(name = "initializer", children = emptyList(), location = Location.empty())
    }
    return SyntaxNode(
        name = "initializer",
        children =
            listOf(
                SyntaxNode(
                    name = "var-init",
                    children = listOf(expression),
                    location = expression.location,
                ),
            ),
        location = expression.location,
    )
}

fun number(
    value: String,
    startCol: Int,
): SyntaxNode = wrap("number", leaf("NUMBER_LITERAL", value, startCol))

fun plus(col: Int): SyntaxNode = leaf("OPERATOR", "+", col)

fun star(col: Int): SyntaxNode = leaf("OPERATOR", "*", col)

fun expression(vararg children: SyntaxNode): SyntaxNode =
    SyntaxNode(
        name = "expression",
        children = children.toList(),
        location = span(children.toList()),
    )

fun term(vararg children: SyntaxNode): SyntaxNode =
    SyntaxNode(
        name = "term",
        children = children.toList(),
        location = span(children.toList()),
    )

fun program(vararg statements: SyntaxNode): SyntaxProgram =
    SyntaxProgram(statements.toList(), span(statements.toList()))

fun addition(
    leftCol: Int,
    opCol: Int,
    rightCol: Int,
    left: String = "1",
    right: String = "2",
): SyntaxNode =
    expression(
        wrap("term", number(left, leftCol)),
        plus(opCol),
        wrap("term", number(right, rightCol)),
    )

private fun span(nodes: List<SyntaxNode>): Location {
    if (nodes.isEmpty()) {
        return Location.empty()
    }

    return Location(nodes.first().location.start, nodes.last().location.end)
}
