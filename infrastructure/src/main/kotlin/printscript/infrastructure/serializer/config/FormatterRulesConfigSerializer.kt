package printscript.infrastructure.serializer.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import printscript.domain.FormatRuleSpec
import printscript.domain.FormatterRulesConfig

@Serializable
private data class FormatterRulesConfigSurrogate(
    val rules: List<FormatRuleSpecSurrogate> = emptyList(),
)

@Serializable
private data class FormatRuleSpecSurrogate(
    val type: String,
    val enabled: Boolean? = null,
    val count: Int? = null,
)

object FormatterRulesConfigSerializer : KSerializer<FormatterRulesConfig> {
    private val surrogateSerializer = FormatterRulesConfigSurrogate.serializer()

    override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

    override fun serialize(
        encoder: Encoder,
        value: FormatterRulesConfig,
    ) {
        val surrogate =
            FormatterRulesConfigSurrogate(
                rules =
                    value.rules
                        .map { spec ->
                            FormatRuleSpecSurrogate(
                                type = spec.type,
                                enabled = spec.enabled,
                                count = spec.count,
                            )
                        },
            )

        encoder.encodeSerializableValue(surrogateSerializer, surrogate)
    }

    override fun deserialize(decoder: Decoder): FormatterRulesConfig {
        val surrogate = decoder.decodeSerializableValue(surrogateSerializer)

        return FormatterRulesConfig(
            rules =
                surrogate.rules
                    .map { spec ->
                        FormatRuleSpec(
                            type = spec.type,
                            enabled = spec.enabled,
                            count = spec.count,
                        )
                    },
        )
    }
}
