package printscript.serializer.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import printscript.domain.OrRule

@Serializable
private data class OrRuleSurrogate(
    val or: List<String>,
)

object OrRuleSerializer : KSerializer<OrRule> {
    private val surrogateSerializer = OrRuleSurrogate.serializer()

    override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

    override fun serialize(
        encoder: Encoder,
        value: OrRule,
    ) {
        val surrogate = OrRuleSurrogate(value.alternatives)
        encoder.encodeSerializableValue(surrogateSerializer, surrogate)
    }

    override fun deserialize(decoder: Decoder): OrRule {
        val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
        return OrRule(surrogate.or)
    }
}
