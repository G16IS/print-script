package printscript.io.channel

import printscript.InputChannel

class ConsoleInputChannel : InputChannel {
    override fun input(askMessage: String): String {
        print(askMessage)
        val input: String = readln()
        return input
    }
}
