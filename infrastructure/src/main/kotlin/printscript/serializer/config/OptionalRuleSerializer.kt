package printscript.serializer.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import printscript.domain.OptionalRule

@Serializable
private data class OptionalRuleSurrogate(
    val optional: String,
)

object OptionalRuleSerializer : KSerializer<OptionalRule> {
    private val surrogateSerializer = OptionalRuleSurrogate.serializer()

    override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

    override fun serialize(
        encoder: Encoder,
        value: OptionalRule,
    ) {
        val surrogate = OptionalRuleSurrogate(value.item)
        encoder.encodeSerializableValue(surrogateSerializer, surrogate)
    }

    override fun deserialize(decoder: Decoder): OptionalRule {
        val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
        return OptionalRule(surrogate.optional)
    }
}
