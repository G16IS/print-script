package printscript.util

fun readResource(path: String): String {
    val stream = object {}.javaClass.classLoader
        .getResourceAsStream(path.removePrefix("/"))
        ?: error("No se encontró el recurso: $path")

    return stream.bufferedReader().use { it.readText() }
}