package printscript.typechecker.handlers

import printscript.domain.Operation
import printscript.domain.TypeSystemConfig
import printscript.error.TypeError
import printscript.syntax.SyntaxNode
import printscript.typechecker.ExpressionTypeResolver
import printscript.typechecker.ScopeStack
import printscript.util.Result
import printscript.util.flatMap

class BinaryOrPrimaryHandler(
    private val resolver: ExpressionTypeResolver,
) : ExpressionKindHandler {
    override val kind: String = "binary-or-primary"

    override fun resolve(
        node: SyntaxNode,
        scope: ScopeStack,
        config: TypeSystemConfig,
    ): Result<String, TypeError> {
        val parts = binaryParts(node)

        return if (parts == null) {
            resolvePrimary(node, scope, config)
        } else {
            resolveBinary(parts, node, scope, config)
        }
    }

    private fun resolvePrimary(
        node: SyntaxNode,
        scope: ScopeStack,
        config: TypeSystemConfig,
    ): Result<String, TypeError> {
        val child = node.children.singleOrNull()

        return if (child == null) {
            Result.Err(TypeError("Expresión primaria inválida", node.location))
        } else {
            resolver.resolve(child, scope, config)
        }
    }

    private fun resolveBinary(
        parts: Triple<SyntaxNode, SyntaxNode, SyntaxNode>,
        node: SyntaxNode,
        scope: ScopeStack,
        config: TypeSystemConfig,
    ): Result<String, TypeError> {
        val (lhs, operator, rhs) = parts

        val op =
            operator.token?.value?.orElse(null) ?: return Result.Err(
                TypeError("Operador inválido", node.location),
            )

        return resolver.resolve(lhs, scope, config).flatMap { leftType ->
            resolver.resolve(rhs, scope, config).flatMap { rightType ->
                match(op, leftType, rightType, node, config)
            }
        }
    }

    private fun match(
        op: String,
        leftType: String,
        rightType: String,
        node: SyntaxNode,
        config: TypeSystemConfig,
    ): Result<String, TypeError> {
        val result = resultType(op, leftType, rightType, config.operations)

        return if (result == null) {
            Result.Err(
                TypeError("El operador '$op' no acepta $leftType y $rightType", node.location),
            )
        } else {
            Result.Ok(result)
        }
    }

    private fun resultType(
        op: String,
        leftType: String,
        rightType: String,
        operations: List<Operation>,
    ): String? {
        val exact =
            operations
                .firstOrNull {
                    it.op == op &&
                        it.operands == listOf(leftType, rightType)
                }

        val swapped =
            if (leftType != rightType) {
                operations.firstOrNull {
                    it.commutative &&
                        it.op == op &&
                        it.operands == listOf(rightType, leftType)
                }
            } else {
                null
            }

        return exact?.result ?: swapped?.result
    }

    private fun binaryParts(node: SyntaxNode): Triple<SyntaxNode, SyntaxNode, SyntaxNode>? {
        val children = node.children
        if (children.size != BINARY_CHILD_COUNT) return null

        return Triple(children[0], children[1], children[2])
    }

    private companion object {
        const val BINARY_CHILD_COUNT = 3
    }
}
