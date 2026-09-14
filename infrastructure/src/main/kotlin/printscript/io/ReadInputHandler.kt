package printscript.io

import printscript.InputChannel
import printscript.ReadInputEffect
import printscript.SideEffect
import printscript.SideEffectHandler
import printscript.io.channel.ConsoleInputChannel

class ReadInputHandler(
    val inputChannel: InputChannel = ConsoleInputChannel(),
) : SideEffectHandler {
    override fun applies(effect: SideEffect): Boolean = effect is ReadInputEffect

    override fun handle(effect: SideEffect): String? {
        assert(effect is ReadInputEffect) {
            "Error: Expected ReadInputEffect"
        }

        val effect = effect as ReadInputEffect

        return inputChannel.input(effect.prompt)
    }
}
