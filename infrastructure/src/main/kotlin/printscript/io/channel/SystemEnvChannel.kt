package printscript.io.channel

import printscript.definitions.EnvChannel

class SystemEnvChannel : EnvChannel {
    override fun read(name: String): String? = System.getenv(name)
}
