package printscript.serializer.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import printscript.domain.ExactRule
import printscript.domain.RegexRule
import printscript.domain.TokenRule

object TokenRuleSerializer : KSerializer<TokenRule> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("TokenRule")

    override fun deserialize(decoder: Decoder): TokenRule {
        val json = decoder.asJsonDecoder()
        val element = json.decodeJsonElement()
        return json.json.decodeFromJsonElement(select(element), element)
    }

    override fun serialize(
        encoder: Encoder,
        value: TokenRule,
    ) {
        val json = encoder.asJsonEncoder()
        json.encodeJsonElement(json.json.encodeToJsonElement(select(value), value))
    }

    private fun select(element: JsonElement): KSerializer<out TokenRule> {
        val type =
            element.jsonObject["type"]?.jsonPrimitive?.content
                ?: error("TokenRule missing type")
        return when (type) {
            "exact" -> ExactRuleSerializer
            "regex" -> RegexRuleSerializer
            else -> error("Unknown token rule type: $type")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun select(rule: TokenRule): KSerializer<TokenRule> = serializerFor(rule) as KSerializer<TokenRule>

    private fun serializerFor(rule: TokenRule): KSerializer<out TokenRule> =
        when (rule) {
            is ExactRule -> ExactRuleSerializer
            is RegexRule -> RegexRuleSerializer
        }
}
