package printscript.expression.binaryoperation

import kotlin.reflect.KClass
import printscript.NumberValue
import printscript.RuntimeValue
import printscript.StringValue

class DefaultTypeConfiguration : TypeConfiguration {
    private val rules: Map<Triple<String, KClass<out RuntimeValue>, KClass<out RuntimeValue>>, BinaryOperationRule> =
        buildRules().associateBy { Triple(it.operator, it.leftType, it.rightType) }

    override fun resolveBinaryOperation(
        operator: String,
        left: KClass<out RuntimeValue>,
        right: KClass<out RuntimeValue>,
    ): BinaryOperationRule? = rules[Triple(operator, left, right)]

    private fun buildRules(): List<BinaryOperationRule> =
        buildList {
            add(numeric("+") { a, b -> a + b })
            add(numeric("-") { a, b -> a - b })
            add(numeric("*") { a, b -> a * b })
            add(numeric("/") { a, b -> a / b })
            add(
                BinaryOperationRule("+", StringValue::class, StringValue::class, StringValue::class) { l, r ->
                    StringValue((l as StringValue).value + (r as StringValue).value)
                },
            )
        }

    private fun numeric(
        op: String,
        compute: (Double, Double) -> Double,
    ) = BinaryOperationRule(op, NumberValue::class, NumberValue::class, NumberValue::class) { l, r ->
        NumberValue(compute((l as NumberValue).value, (r as NumberValue).value))
    }
}
