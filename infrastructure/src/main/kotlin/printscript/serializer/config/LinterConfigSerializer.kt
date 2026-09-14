package printscript.serializer.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import printscript.domain.LinterConfig
import printscript.domain.RuleConfig

@Serializable
private data class RuleConfigSurrogate(
    val enabled: Boolean = true,
    val options: Map<String, String> = emptyMap(),
)

object RuleConfigSerializer : KSerializer<RuleConfig> {
    private val surrogateSerializer = RuleConfigSurrogate.serializer()

    override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

    override fun serialize(
        encoder: Encoder,
        value: RuleConfig,
    ) {
        val surrogate =
            RuleConfigSurrogate(
                enabled = value.enabled,
                options = value.options,
            )
        encoder.encodeSerializableValue(surrogateSerializer, surrogate)
    }

    override fun deserialize(decoder: Decoder): RuleConfig {
        val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
        return RuleConfig(
            enabled = surrogate.enabled,
            options = surrogate.options,
        )
    }
}

@Serializable
private data class LinterConfigSurrogate(
    val rules: Map<
        String,
        @Serializable(with = RuleConfigSerializer::class)
        RuleConfig,
    > = emptyMap(),
)

object LinterConfigSerializer : KSerializer<LinterConfig> {
    private val surrogateSerializer = LinterConfigSurrogate.serializer()

    override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

    override fun serialize(
        encoder: Encoder,
        value: LinterConfig,
    ) {
        val surrogate =
            LinterConfigSurrogate(
                rules = value.rules,
            )
        encoder.encodeSerializableValue(surrogateSerializer, surrogate)
    }

    override fun deserialize(decoder: Decoder): LinterConfig {
        val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
        return LinterConfig(
            rules = surrogate.rules,
        )
    }
}
