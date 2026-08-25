package printscript.expression.binaryoperation

import kotlin.reflect.KClass
import printscript.RuntimeValue

interface TypeConfiguration {
    fun resolveBinaryOperation(
        operator: String,
        left: KClass<out RuntimeValue>,
        right: KClass<out RuntimeValue>,
    ): BinaryOperationRule?
}
