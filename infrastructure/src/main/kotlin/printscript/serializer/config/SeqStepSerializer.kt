package printscript.serializer.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import printscript.domain.RuleRefStep
import printscript.domain.SeqStep
import printscript.domain.TokenStep

object SeqStepSerializer : KSerializer<SeqStep> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SeqStep")

    override fun deserialize(decoder: Decoder): SeqStep = parse(decoder.asJsonDecoder().decodeJsonElement())

    override fun serialize(
        encoder: Encoder,
        value: SeqStep,
    ) {
        encoder.asJsonEncoder().encodeJsonElement(toJson(value))
    }

    private fun parse(element: JsonElement): SeqStep =
        when (element) {
            is JsonPrimitive -> TokenStep(element.content, capture = false)
            is JsonObject -> parseObject(element)
            else -> error("Invalid seq step: $element")
        }

    private fun parseObject(obj: JsonObject): SeqStep =
        when {
            "capture" in obj -> TokenStep(string(obj, "capture"), capture = true)
            "rule" in obj -> RuleRefStep(string(obj, "rule"))
            else -> error("Invalid seq step object: $obj")
        }

    private fun toJson(step: SeqStep): JsonElement =
        when (step) {
            is TokenStep -> tokenJson(step)
            is RuleRefStep -> JsonObject(mapOf("rule" to JsonPrimitive(step.name)))
            else -> error("Unknown seq step: $step")
        }

    private fun tokenJson(step: TokenStep): JsonElement {
        if (!step.capture) return JsonPrimitive(step.type)
        return JsonObject(mapOf("capture" to JsonPrimitive(step.type)))
    }

    private fun string(
        obj: JsonObject,
        key: String,
    ): String = obj[key]?.jsonPrimitive?.contentOrNull ?: error("Missing '$key' in seq step")
}
