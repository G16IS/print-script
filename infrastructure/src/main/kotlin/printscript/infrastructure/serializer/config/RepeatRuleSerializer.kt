package printscript.infrastructure.serializer.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import printscript.domain.RepeatRule

@Serializable
private data class RepeatRuleSurrogate(
    val repeat: String,
)

object RepeatRuleSerializer : KSerializer<RepeatRule> {
    private val surrogateSerializer = RepeatRuleSurrogate.serializer()

    override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

    override fun serialize(
        encoder: Encoder,
        value: RepeatRule,
    ) {
        val surrogate = RepeatRuleSurrogate(value.item)
        encoder.encodeSerializableValue(surrogateSerializer, surrogate)
    }

    override fun deserialize(decoder: Decoder): RepeatRule {
        val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
        return RepeatRule(surrogate.repeat)
    }
}
