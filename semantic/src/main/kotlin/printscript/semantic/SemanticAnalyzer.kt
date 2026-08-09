package printscript.semantic

import printscript.common.ast.Expression
import printscript.common.ast.ExpressionStatement
import printscript.common.ast.NumberLiteral
import printscript.common.ast.Statement
import printscript.common.ast.StringLiteral
import printscript.common.ast.VariableDeclaration
import printscript.common.ast.VariableStatement
import printscript.common.VariableType
import printscript.common.ast.BinaryExpression

class SemanticAnalyzer {
    private val symbolTable = SymbolTable()
    private val errors = mutableListOf<SemanticError>()


    fun analyze(program: List<Statement>): List<SemanticError> {
        for (statement in program) {
            visit(statement)
        }
        return errors
    }

    fun visit(statement : Statement){
        when(statement){
            is VariableStatement -> visitVariableDeclaration(statement.declaration)
            is ExpressionStatement -> visitExpressionStatement(statement)
            else -> {}
        }
    }

    private fun visitVariableDeclaration(node: VariableDeclaration) {
        val initializer = node.initializer

        if (initializer != null) {
            val actualType = inferSimpleType(initializer)
            if (actualType != null && actualType != node.declaredType) {
                errors.add(SemanticError(
                    messageError = "Se esperaba ${node.declaredType} pero se encontró $actualType",
                    location = initializer.location,
                ))
            }
        }

        symbolTable.declare(node.id.name, node.declaredType)
    }

    private fun visitExpressionStatement(node : ExpressionStatement){
        inferSimpleType(node.expression)
    }

    private fun inferSimpleType(expr: Expression): VariableType? = when (expr) {
        is NumberLiteral -> VariableType.NUMBER
        is StringLiteral -> VariableType.STRING
        else -> null
    }

    private fun inferBinaryType(expr: BinaryExpression): VariableType? {
        val leftType = inferSimpleType(expr.left)
        val rightType = inferSimpleType(expr.right)

        if (leftType == null || rightType== null) {return null}

        return when (expr.operation) {
            ""+"" -> when (leftType) {
                VariableType.NUMBER if rightType == VariableType.NUMBER -> VariableType.NUMBER
                VariableType.STRING if rightType == VariableType.STRING -> VariableType.STRING
                else -> null
            }
            "-","/" -> {
                if (leftType == VariableType.NUMBER && rightType == VariableType.NUMBER) VariableType.NUMBER
                else {errors.add(SemanticError(
                    messageError = "Operador '${expr.operation}' requiere operandos number",
                    location = expr.location,
                ))}
                null
            }
            "*" -> when (leftType) {
                VariableType.NUMBER if rightType == VariableType.NUMBER -> VariableType.NUMBER
                VariableType.NUMBER if rightType == VariableType.STRING -> VariableType.STRING
                VariableType.STRING if rightType == VariableType.NUMBER -> VariableType.STRING
                else -> null
            }
            else -> null
        }
    }


    fun getErrors(): List<SemanticError> = errors
}