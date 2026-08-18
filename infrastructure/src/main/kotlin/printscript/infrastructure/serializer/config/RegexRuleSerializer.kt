package printscript.infrastructure.serializer.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import printscript.domain.RegexRule

@Serializable
@SerialName("regex")
private data class RegexRuleSurrogate(
    val matcher: List<String>,
    val token: String,
    val capture: Boolean,
    val partial: String
)

object RegexRuleSerializer : KSerializer<RegexRule> {

    private val surrogateSerializer = RegexRuleSurrogate.serializer()

    override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

    override fun serialize(encoder: Encoder, value: RegexRule) {
        val surrogate = RegexRuleSurrogate(value.matcher, value.token, value.capture, value.partial)

        encoder.encodeSerializableValue(surrogateSerializer, surrogate)
    }

    override fun deserialize(decoder: Decoder): RegexRule {
        val surrogate = decoder.decodeSerializableValue(surrogateSerializer)

        return RegexRule(surrogate.matcher, surrogate.token, surrogate.capture, surrogate.partial)
    }
}