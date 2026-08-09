package printscript.common.ast

import printscript.common.reader.CharPosition

data class Location(
    val start: CharPosition,
    val end: CharPosition
)


sealed interface Node {
    val location: Location
}

sealed interface Statement : Node

sealed interface ExpressionStatement : Statement {
    val expression: Expression
}

data class VariableStatement(
    val declaration: VariableDeclaration,
    override val location: Location
) : Statement

data class VariableDeclaration(
    val id: Identifier, val value: String,
    override val location: Location
) : Node

sealed interface Expression : Node

data class Identifier(val name: String, override val location: Location) : Expression
data class CallExpression(
    val callee: String, val args: Array<String>,
    override val location: Location
) : Expression

data class NumberLiteral(val value: Double, override val location: Location) : Expression
data class StringLiteral(val value: String, override val location: Location) : Expression

data class BinaryExpression(val left: Expression, val right: Expression, val operation: String)
