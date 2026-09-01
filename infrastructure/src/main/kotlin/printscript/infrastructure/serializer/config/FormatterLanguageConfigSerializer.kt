package printscript.infrastructure.serializer.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import printscript.domain.FormatterLanguageConfig
import printscript.domain.LanguageFormatRuleSpec
import printscript.domain.UserRuleBinding

@Serializable
private data class FormatterLanguageConfigSurrogate(
    val rules: List<LanguageFormatRuleSpecSurrogate> = emptyList(),
    val userBindings: List<UserRuleBindingSurrogate> = emptyList(),
)

@Serializable
private data class LanguageFormatRuleSpecSurrogate(
    val type: String,
    val token: String,
    val value: String? = null,
    val previous: String? = null,
)

@Serializable
private data class UserRuleBindingSurrogate(
    val userType: String,
    val type: String,
    val token: String,
    val value: String? = null,
    val previous: String? = null,
    val param: String,
    val default: JsonElement? = null,
)

object FormatterLanguageConfigSerializer : KSerializer<FormatterLanguageConfig> {
    private val surrogateSerializer = FormatterLanguageConfigSurrogate.serializer()

    override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

    override fun serialize(
        encoder: Encoder,
        value: FormatterLanguageConfig,
    ) {
        val surrogate =
            FormatterLanguageConfigSurrogate(
                rules = value.rules.map { it.toSurrogate() },
                userBindings = value.userBindings.map { it.toSurrogate() },
            )
        encoder.encodeSerializableValue(surrogateSerializer, surrogate)
    }

    override fun deserialize(decoder: Decoder): FormatterLanguageConfig {
        val surrogate = decoder.decodeSerializableValue(surrogateSerializer)
        return FormatterLanguageConfig(
            rules = surrogate.rules.map { it.toDomain() },
            userBindings = surrogate.userBindings.map { it.toDomain() },
        )
    }
}

private fun LanguageFormatRuleSpec.toSurrogate() =
    LanguageFormatRuleSpecSurrogate(
        type = type,
        token = token,
        value = value,
        previous = previous,
    )

private fun LanguageFormatRuleSpecSurrogate.toDomain() =
    LanguageFormatRuleSpec(
        type = type,
        token = token,
        value = value,
        previous = previous,
    )

private fun UserRuleBinding.toSurrogate() =
    UserRuleBindingSurrogate(
        userType = userType,
        type = type,
        token = token,
        value = value,
        previous = previous,
        param = param,
        default = defaultElement(),
    )

private fun UserRuleBinding.defaultElement(): JsonPrimitive? =
    when {
        defaultEnabled != null -> JsonPrimitive(defaultEnabled)
        defaultCount != null -> JsonPrimitive(defaultCount)
        else -> null
    }

private fun UserRuleBindingSurrogate.toDomain(): UserRuleBinding {
    val primitive = default?.jsonPrimitive
    return UserRuleBinding(
        userType = userType,
        type = type,
        token = token,
        value = value,
        previous = previous,
        param = param,
        defaultEnabled = primitive?.booleanOrNull,
        defaultCount = primitive?.intOrNull,
    )
}
