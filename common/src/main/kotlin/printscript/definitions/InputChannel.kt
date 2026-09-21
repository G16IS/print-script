package printscript.definitions

interface InputChannel {
    fun input(askMessage: String): String
}
