package printscript.io.channel

import printscript.definitions.PrintChannel

class ConsolePrintChannel : PrintChannel {
    override fun print(text: String) {
        println(text)
    }
}
