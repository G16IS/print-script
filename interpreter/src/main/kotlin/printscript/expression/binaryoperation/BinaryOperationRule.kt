package printscript.expression.binaryoperation

import kotlin.reflect.KClass
import printscript.RuntimeValue

data class BinaryOperationRule(
    val operator: String,
    val leftType: KClass<out RuntimeValue>,
    val rightType: KClass<out RuntimeValue>,
    val resultType: KClass<out RuntimeValue>,
    val apply: (RuntimeValue, RuntimeValue) -> RuntimeValue,
)
