package printscript

import printscript.ast.BinaryExpression
import printscript.ast.CallExpression
import printscript.ast.Expression
import printscript.ast.Identifier
import printscript.ast.NumberLiteral
import printscript.ast.StringLiteral
import printscript.ast.VariableType

class ExpressionTypeChecker(
    private val context: SemanticContext,
) {
    fun typeOf(expression: Expression): VariableType? =
        when (expression) {
            is NumberLiteral -> VariableType.NUMBER
            is StringLiteral -> VariableType.STRING
            is Identifier -> typeOfIdentifier(expression)
            is BinaryExpression -> typeOfBinary(expression)
            is CallExpression -> typeOfCall(expression)
        }

    private fun typeOfBinary(expression: BinaryExpression): VariableType? {
        val left = typeOf(expression.left)
        val right = typeOf(expression.right)

        if (left == null || right == null) return null

        return when (expression.operation) {
            "+" ->
                when {
                    left == VariableType.NUMBER &&
                        right == VariableType.NUMBER -> VariableType.NUMBER

                    left == VariableType.STRING &&
                        right == VariableType.STRING -> VariableType.STRING

                    else -> reportInvalidOperands(expression, left, right)
                }

            "-", "/", "*" -> {
                if (
                    left == VariableType.NUMBER &&
                    right == VariableType.NUMBER
                ) {
                    VariableType.NUMBER
                } else {
                    reportInvalidOperands(expression, left, right)
                }
            }

            else -> {
                context.errors.add(
                    SemanticError(
                        messageError =
                            "Operador desconocido '${expression.operation}'",
                        location = expression.location,
                    ),
                )
                null
            }
        }
    }

    private fun typeOfIdentifier(identifier: Identifier): VariableType? {
        val type = context.symbolTable.typeOf(identifier.name)

        if (type == null) {
            context.errors.add(
                SemanticError(
                    messageError = "Variable '${identifier.name}' no declarada",
                    location = identifier.location,
                ),
            )
        }

        return type
    }

    private fun typeOfCall(expression: CallExpression): VariableType? {
        for (argument in expression.args) {
            typeOf(argument)
        }

        return null
    }

    private fun reportInvalidOperands(
        expression: BinaryExpression,
        left: VariableType,
        right: VariableType,
    ): VariableType? {
        context.errors.add(
            SemanticError(
                messageError =
                    "El operador '${expression.operation}' no acepta $left y $right",
                location = expression.location,
            ),
        )

        return null
    }
}
