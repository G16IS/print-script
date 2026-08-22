package printscript.infrastructure.serializer.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import printscript.domain.Grammar
import printscript.domain.GrammarRule

@Serializable
private data class GrammarSurrogate(
    val start: String,
    val rules: Map<
        String,
        @Serializable(with = GrammarRuleSerializer::class)
        GrammarRule,
    >,
)

object GrammarSerializer : KSerializer<Grammar> {
    private val surrogateSerializer = GrammarSurrogate.serializer()

    override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

    override fun serialize(
        encoder: Encoder,
        value: Grammar,
    ) {
        val surrogate = GrammarSurrogate(value.start, value.rules)
        encoder.encodeSerializableValue(surrogateSerializer, surrogate)
    }

    override fun deserialize(decoder: Decoder): Grammar {
        val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
        return Grammar(surrogate.start, surrogate.rules)
    }
}
