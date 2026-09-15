package printscript.serializer.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import printscript.domain.TryRule

@Serializable
private data class TryRuleSurrogate(
    val `try`: String,
)

object TryRuleSerializer : KSerializer<TryRule> {
    private val surrogateSerializer = TryRuleSurrogate.serializer()

    override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

    override fun serialize(
        encoder: Encoder,
        value: TryRule,
    ) {
        val surrogate = TryRuleSurrogate(value.item)
        encoder.encodeSerializableValue(surrogateSerializer, surrogate)
    }

    override fun deserialize(decoder: Decoder): TryRule {
        val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
        return TryRule(surrogate.`try`)
    }
}
