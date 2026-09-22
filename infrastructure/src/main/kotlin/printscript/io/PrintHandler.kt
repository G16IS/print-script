package printscript.io

import printscript.definitions.PrintChannel
import printscript.definitions.PrintEffect
import printscript.definitions.SideEffect
import printscript.definitions.SideEffectHandler
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
