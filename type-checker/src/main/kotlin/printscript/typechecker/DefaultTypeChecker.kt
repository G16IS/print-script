package printscript.typechecker

import printscript.domain.TypeSystemConfig
import printscript.syntax.SyntaxNode
import printscript.syntax.SyntaxProgram
import printscript.typechecker.handlers.AssignmentHandler
import printscript.typechecker.handlers.DeclarationHandler
import printscript.typechecker.handlers.ExpressionStmtHandler
import printscript.typechecker.handlers.IfHandler
import printscript.typechecker.handlers.NodeHandler
import printscript.typechecker.handlers.StatementCheck
import printscript.util.Report
import printscript.util.Result

class DefaultTypeChecker(
    private val config: TypeSystemConfig,
    private val resolver: ExpressionTypeResolver,
    statementHandlers: List<NodeHandler>? = null,
) : TypeChecker {
    private val handlers: Map<String, NodeHandler> =
        (statementHandlers ?: builtInHandlers()).associateBy { it.kind }

    override fun check(program: SyntaxProgram): Report<SyntaxProgram, TypeError> {
        var scope = ScopeStack()
        val errors = mutableListOf<TypeError>()

        for (statement in program.statements) {
            val checked = checkStatement(statement, scope)
            errors += checked.errors
            scope = checked.scope
        }

        return Report(program, errors)
    }

    override fun checkStrict(program: SyntaxProgram): Result<SyntaxProgram, TypeError> {
        var scope = ScopeStack()

        for (statement in program.statements) {
            val checked = checkStatement(statement, scope)
            val first = checked.errors.firstOrNull()
            if (first != null) return Result.Err(first)
            scope = checked.scope
        }

        return Result.Ok(program)
    }

    private fun checkStatement(
        statement: SyntaxNode,
        scope: ScopeStack,
    ): StatementCheck {
        val nodeConfig =
            config.nodes[statement.name] ?: return StatementCheck(
                scope,
                listOf(TypeError("Nodo no reconocido: '${statement.name}'", statement.location)),
            )

        val handler = handlers[nodeConfig.kind]

        return handler?.check(statement, scope, config)
            ?: StatementCheck(
                scope,
                listOf(
                    TypeError(
                        "Kind '${nodeConfig.kind}' no soportado",
                        statement.location,
                    ),
                ),
            )
    }

    private fun builtInHandlers(): List<NodeHandler> =
        listOf(
            DeclarationHandler(resolver),
            ExpressionStmtHandler(resolver),
            IfHandler(resolver) { stmt, sc -> checkStatement(stmt, sc) },
            AssignmentHandler(resolver),
        )
}
