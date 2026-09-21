package printscript.typechecker

import printscript.domain.TypeSystemConfig
import printscript.error.TypeError
import printscript.syntax.SyntaxNode
import printscript.syntax.SyntaxProgram
import printscript.util.Result

interface TypeChecker {
    companion object {
        fun create(
            config: TypeSystemConfig,
            kindHandlerFactory: ExpressionKindHandlerFactory,
        ) = DefaultTypeChecker(config, ExpressionTypeResolver(kindHandlerFactory))
    }

    fun check(program: SyntaxProgram): Result<SyntaxProgram, TypeError>

    fun checkNode(
        statement: SyntaxNode,
        scope: ScopeStack,
    ): Result<Pair<ScopeStack, SyntaxNode>, NodeCheckError>
}

data class NodeCheckError(
    val scope: ScopeStack,
    val error: TypeError,
)
