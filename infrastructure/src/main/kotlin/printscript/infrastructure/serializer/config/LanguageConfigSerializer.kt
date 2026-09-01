package printscript.infrastructure.serializer.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import printscript.domain.LanguageConfig
import printscript.domain.TokenRule

@Serializable
private data class LanguageConfigSurrogate(
    val order: List<String>,
    val config: Map<
        String,
        List<
            @Serializable(with = TokenRuleSerializer::class)
            TokenRule,
        >,
    >,
)

object LanguageConfigSerializer : KSerializer<LanguageConfig> {
    private val surrogateSerializer = LanguageConfigSurrogate.serializer()

    override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

    override fun serialize(
        encoder: Encoder,
        value: LanguageConfig,
    ) {
        val surrogate = LanguageConfigSurrogate(value.order, value.config)
        encoder.encodeSerializableValue(surrogateSerializer, surrogate)
    }

    override fun deserialize(decoder: Decoder): LanguageConfig {
        val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
        return LanguageConfig(surrogate.order, surrogate.config)
    }
}
