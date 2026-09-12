package printscript.serializer.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import printscript.domain.AtomRule
import printscript.domain.GrammarRule
import printscript.domain.LeftRule
import printscript.domain.OrRule
import printscript.domain.RepeatRule
import printscript.domain.SeqRule

object GrammarRuleSerializer : KSerializer<GrammarRule> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("GrammarRule")

    override fun deserialize(decoder: Decoder): GrammarRule {
        val json = decoder.asJsonDecoder()
        val element = json.decodeJsonElement()
        return json.json.decodeFromJsonElement(select(element), element)
    }

    override fun serialize(
        encoder: Encoder,
        value: GrammarRule,
    ) {
        val json = encoder.asJsonEncoder()
        json.encodeJsonElement(json.json.encodeToJsonElement(select(value), value))
    }

    private fun select(element: JsonElement): KSerializer<out GrammarRule> {
        val keys = element.jsonObject.keys
        return when {
            "or" in keys -> OrRuleSerializer
            "seq" in keys -> SeqRuleSerializer
            "left" in keys -> LeftRuleSerializer
            "atom" in keys -> AtomRuleSerializer
            "repeat" in keys -> RepeatRuleSerializer
            else -> error("Unknown grammar rule keys: $keys")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun select(rule: GrammarRule): KSerializer<GrammarRule> = serializerFor(rule) as KSerializer<GrammarRule>

    private fun serializerFor(rule: GrammarRule): KSerializer<out GrammarRule> =
        when (rule) {
            is OrRule -> OrRuleSerializer
            is SeqRule -> SeqRuleSerializer
            is LeftRule -> LeftRuleSerializer
            is AtomRule -> AtomRuleSerializer
            is RepeatRule -> RepeatRuleSerializer
            else -> error("Unknown grammar rule: ${rule::class.simpleName}")
        }
}
