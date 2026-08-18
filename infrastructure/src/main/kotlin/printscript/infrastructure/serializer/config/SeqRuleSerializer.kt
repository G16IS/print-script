package printscript.infrastructure.serializer.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import printscript.domain.SeqRule
import printscript.domain.SeqStep

@Serializable
private data class SeqRuleSurrogate(
    val seq: List<@Serializable(with = SeqStepSerializer::class) SeqStep>
)

object SeqRuleSerializer : KSerializer<SeqRule> {
    private val surrogateSerializer = SeqRuleSurrogate.serializer()

    override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

    override fun serialize(encoder: Encoder, value: SeqRule) {
        val surrogate = SeqRuleSurrogate(value.steps)
        encoder.encodeSerializableValue(surrogateSerializer, surrogate)
    }

    override fun deserialize(decoder: Decoder): SeqRule {
        val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
        return SeqRule(surrogate.seq)
    }
}
