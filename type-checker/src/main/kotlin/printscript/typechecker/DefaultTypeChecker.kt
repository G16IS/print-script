package printscript.typechecker

import printscript.domain.TypeSystemConfig
import printscript.error.TypeError
import printscript.syntax.SyntaxNode
import printscript.syntax.SyntaxProgram
import printscript.typechecker.node.AssignmentHandler
import printscript.typechecker.node.Checked
import printscript.typechecker.node.DeclarationHandler
import printscript.typechecker.node.ExpressionStmtHandler
import printscript.typechecker.node.IfHandler
import printscript.typechecker.node.NodeHandler
import printscript.util.Result
import printscript.util.err
import printscript.util.ok

class DefaultTypeChecker(
    private val config: TypeSystemConfig,
    private val resolver: ExpressionTypeResolver,
    statementHandlers: List<NodeHandler>? = null,
) : TypeChecker {
    private val handlers: Map<String, NodeHandler> =
        (statementHandlers ?: builtInHandlers()).associateBy { it.kind }

    override fun check(program: SyntaxProgram): Result<SyntaxProgram, TypeError> {
        var scope = ScopeStack()

        for (statement in program.statements) {
            when (val checked = checkNode(statement, scope)) {
                is Result.Err -> return err(checked.error.error)
                is Result.Ok -> scope = checked.value.first
            }
        }

        return ok(program)
    }

    override fun checkNode(
        statement: SyntaxNode,
        scope: ScopeStack,
    ): Result<Pair<ScopeStack, SyntaxNode>, NodeCheckError> {
        val checked = checkStatement(statement, scope)
        val error = checked.error ?: return ok(checked.scope to statement)
        return err(NodeCheckError(checked.scope, error))
    }

    private fun checkStatement(
        statement: SyntaxNode,
        scope: ScopeStack,
    ): Checked {
        val nodeConfig =
            config.nodes[statement.name] ?: return Checked(
                scope,
                TypeError("Nodo no reconocido: '${statement.name}'", statement.location),
            )

        val handler = handlers[nodeConfig.kind]

        return handler?.check(statement, scope, config)
            ?: Checked(
                scope,
                TypeError(
                    "Kind '${nodeConfig.kind}' no soportado",
                    statement.location,
                ),
            )
    }

    fun builtInHandlers(): List<NodeHandler> =
        listOf(
            DeclarationHandler(resolver),
            ExpressionStmtHandler(resolver),
            IfHandler(resolver, ::checkStatement),
            AssignmentHandler(resolver),
        )
}
