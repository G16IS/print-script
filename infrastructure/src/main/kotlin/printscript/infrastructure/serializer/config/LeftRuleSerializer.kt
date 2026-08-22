package printscript.infrastructure.serializer.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import printscript.domain.LeftRule
import printscript.domain.OperatorSpec

@Serializable
private data class OperatorSpecSurrogate(
    val token: String,
    val values: List<String>,
)

@Serializable
private data class LeftRuleSurrogate(
    val left: String,
    val op: OperatorSpecSurrogate,
)

object LeftRuleSerializer : KSerializer<LeftRule> {
    private val surrogateSerializer = LeftRuleSurrogate.serializer()

    override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

    override fun serialize(
        encoder: Encoder,
        value: LeftRule,
    ) {
        val surrogate = toSurrogate(value)
        encoder.encodeSerializableValue(surrogateSerializer, surrogate)
    }

    override fun deserialize(decoder: Decoder): LeftRule {
        val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
        return fromSurrogate(surrogate)
    }

    private fun toSurrogate(value: LeftRule): LeftRuleSurrogate =
        LeftRuleSurrogate(
            left = value.left,
            op = OperatorSpecSurrogate(value.op.token, value.op.values),
        )

    private fun fromSurrogate(surrogate: LeftRuleSurrogate): LeftRule =
        LeftRule(surrogate.left, OperatorSpec(surrogate.op.token, surrogate.op.values))
}
