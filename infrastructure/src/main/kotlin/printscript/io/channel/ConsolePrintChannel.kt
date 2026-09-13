package printscript.io.channel

import printscript.PrintChannel

class ConsolePrintChannel : PrintChannel {
    override fun print(text: String) {
        println(text)
    }
}
