package printscript.io.channel

import printscript.EnvChannel

class SystemEnvChannel : EnvChannel {
    override fun read(name: String): String? = System.getenv(name)
}
