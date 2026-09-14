package printscript.io

import printscript.PrintChannel
import printscript.PrintEffect
import printscript.SideEffect
import printscript.SideEffectHandler
import printscript.io.channel.ConsolePrintChannel

class PrintHandler(
    val printChannel: PrintChannel = ConsolePrintChannel(),
) : SideEffectHandler {
    override fun applies(effect: SideEffect) = effect is PrintEffect

    override fun handle(effect: SideEffect): String? {
        assert(effect is PrintEffect) {
            "Error: Expected PrintEffect"
        }

        val effect = effect as PrintEffect

        printChannel.print(effect.text)
        return null
    }
}
