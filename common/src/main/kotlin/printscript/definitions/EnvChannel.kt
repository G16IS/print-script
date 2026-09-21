package printscript.definitions

/** Puerto de lectura de variables de ambiente. Espejo de [InputChannel]. */
interface EnvChannel {
    fun read(name: String): String?
}
