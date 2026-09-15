package printscript.expression.binaryoperation

import kotlin.math.roundToInt
import kotlin.reflect.KClass
import printscript.NumberValue
import printscript.RuntimeValue
import printscript.StringValue

object DefaultTypeConfiguration : TypeConfiguration {
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
                    if (l is StringValue && r is StringValue) {
                        StringValue(l.value + r.value)
                    } else {
                        null
                    }
                },
            )
            add(
                BinaryOperationRule("+", StringValue::class, NumberValue::class, StringValue::class) { l, r ->
                    if (l is StringValue && r is NumberValue) {
                        StringValue(l.value + r.value.roundToInt().toString())
                    } else {
                        null
                    }
                },
            )
            add(
                BinaryOperationRule("+", NumberValue::class, StringValue::class, StringValue::class) { l, r ->
                    if (l is NumberValue && r is StringValue) {
                        StringValue(l.value.roundToInt().toString() + r.value)
                    } else {
                        null
                    }
                },
            )
        }

    private fun numeric(
        op: String,
        compute: (Double, Double) -> Double,
    ) = BinaryOperationRule(op, NumberValue::class, NumberValue::class, NumberValue::class) { l, r ->
        if (l is NumberValue && r is NumberValue) NumberValue(compute(l.value, r.value)) else null
    }
}
