package printscript.ast

sealed interface Node {
    val location: Location
}

/** Root of a parsed program. */
data class Program(
    val statements: List<Statement>,
    override val location: Location,
) : Node {
    fun withStatement(statement: Statement): Program =
        Program(
            statements = statements + statement,
            location =
                Location(
                    start = location.start,
                    end = statement.location.end,
                ),
        )

    companion object {
        fun empty(): Program =
            Program(
                statements = emptyList(),
                location = Location.empty(),
            )
    }
}

sealed interface Statement : Node

/** Marker for statements whose primary payload is an expression. */
sealed interface ExpressionStmt : Statement {
    val expression: Expression
}

data class VariableStatement(
    val declaration: VariableDeclaration,
    override val location: Location,
) : Statement

data class VariableDeclaration(
    val id: Identifier,
    val typeAnnotation: VariableType,
    val initializer: Expression,
    override val location: Location,
) : Node

/** Statement form of an expression, e.g. `println(x);`. */
data class ExpressionStatement(
    override val expression: Expression,
    override val location: Location,
) : ExpressionStmt

sealed interface Expression : Node

data class Identifier(
    val name: String,
    override val location: Location,
) : Expression

data class CallExpression(
    val callee: String,
    val args: List<Expression>,
    override val location: Location,
) : Expression

data class NumberLiteral(
    val value: Double,
    override val location: Location,
) : Expression

data class StringLiteral(
    val value: String,
    override val location: Location,
) : Expression

data class BinaryExpression(
    val left: Expression,
    val right: Expression,
    val operation: String,
    override val location: Location,
) : Expression
