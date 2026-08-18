package printscript.infrastructure.serializer.config

import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder

internal fun Decoder.asJsonDecoder(): JsonDecoder =
    this as? JsonDecoder ?: error("Grammar JSON decoder required")

internal fun Encoder.asJsonEncoder(): JsonEncoder =
    this as? JsonEncoder ?: error("Grammar JSON encoder required")
