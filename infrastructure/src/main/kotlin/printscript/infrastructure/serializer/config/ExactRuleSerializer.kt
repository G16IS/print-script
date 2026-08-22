package printscript.infrastructure.serializer.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import printscript.domain.ExactRule

@Serializable
@SerialName("exact")
private data class ExactRuleSurrogate(
    val matcher: List<String>,
    val token: String,
    val capture: Boolean,
)

object ExactRuleSerializer : KSerializer<ExactRule> {
    private val surrogateSerializer = ExactRuleSurrogate.serializer()

    override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

    override fun serialize(
        encoder: Encoder,
        value: ExactRule,
    ) {
        val surrogate = ExactRuleSurrogate(value.matcher, value.token, value.capture)

        encoder.encodeSerializableValue(surrogateSerializer, surrogate)
    }

    override fun deserialize(decoder: Decoder): ExactRule {
        val surrogate = decoder.decodeSerializableValue(surrogateSerializer)

        return ExactRule(surrogate.matcher, surrogate.token, surrogate.capture)
    }
}
