package printscript.serializer.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

internal val serializerJson =
    Json {
        ignoreUnknownKeys = true
    }

internal fun <T> decode(
    serializer: KSerializer<T>,
    json: String,
): T = serializerJson.decodeFromString(serializer, json)

internal fun <T> roundTrip(
    serializer: KSerializer<T>,
    value: T,
): T {
    val encoded = serializerJson.encodeToString(serializer, value)
    return serializerJson.decodeFromString(serializer, encoded)
}

internal fun resourceText(name: String): String =
    checkNotNull(object {}.javaClass.classLoader.getResourceAsStream(name)) {
        "Missing resource $name"
    }.bufferedReader()
        .use { it.readText() }
