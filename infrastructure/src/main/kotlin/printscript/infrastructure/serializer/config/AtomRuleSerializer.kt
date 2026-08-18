package printscript.infrastructure.serializer.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import printscript.domain.AtomRule

@Serializable
private data class AtomRuleSurrogate(val atom: String)

object AtomRuleSerializer : KSerializer<AtomRule> {
    private val surrogateSerializer = AtomRuleSurrogate.serializer()

    override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

    override fun serialize(encoder: Encoder, value: AtomRule) {
        val surrogate = AtomRuleSurrogate(value.token)
        encoder.encodeSerializableValue(surrogateSerializer, surrogate)
    }

    override fun deserialize(decoder: Decoder): AtomRule {
        val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
        return AtomRule(surrogate.atom)
    }
}
