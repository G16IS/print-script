package printscript

import printscript.ast.ExpressionStatement
import printscript.ast.Program
import printscript.ast.Statement
import printscript.ast.VariableDeclaration
import printscript.ast.VariableStatement

class DefaultSemanticAnalyzer : SemanticAnalyzer {

    override fun analyze(program: Program): SemanticResult {
        val context = SemanticContext()
        val typeChecker = ExpressionTypeChecker(context)

        for (statement in program.statements) {
            analyzeStatement(
                statement = statement,
                context = context,
                typeChecker = typeChecker,
            )
        }

        return if (context.errors.isEmpty()) {
            SemanticResult.Success(program)
        } else {
            SemanticResult.Failure(context.errors.toList())
        }
    }

    private fun analyzeStatement(
        statement: Statement,
        context: SemanticContext,
        typeChecker: ExpressionTypeChecker,
    ) {
        when (statement) {
            is VariableStatement ->
                analyzeVariableDeclaration(
                    declaration = statement.declaration,
                    context = context,
                    typeChecker = typeChecker,
                )

            is ExpressionStatement ->
                typeChecker.typeOf(statement.expression)
        }
    }

    private fun analyzeVariableDeclaration(
        declaration: VariableDeclaration,
        context: SemanticContext,
        typeChecker: ExpressionTypeChecker,
    ) {
        val initializerType =
            typeChecker.typeOf(declaration.initializer)

        if (
            initializerType != null &&
            initializerType != declaration.typeAnnotation
        ) {
            context.errors.add(
                SemanticError(
                    messageError =
                        "Se esperaba ${declaration.typeAnnotation} " +
                            "pero se encontró $initializerType",
                    location = declaration.initializer.location,
                ),
            )
        }

        val wasDeclared = context.symbolTable.declare(
            name = declaration.id.name,
            type = declaration.typeAnnotation,
        )

        if (!wasDeclared) {
            context.errors.add(
                SemanticError(
                    messageError =
                        "La variable '${declaration.id.name}' " +
                            "ya fue declarada",
                    location = declaration.id.location,
                ),
            )
        }
    }
}
