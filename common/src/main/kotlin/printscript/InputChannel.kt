package printscript

interface InputChannel {
    fun input(askMessage: String): String
}
