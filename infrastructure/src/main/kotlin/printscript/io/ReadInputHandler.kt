package printscript.io

import printscript.definitions.InputChannel
import printscript.definitions.PrintChannel
import printscript.definitions.ReadInputEffect
import printscript.definitions.SideEffect
import printscript.definitions.SideEffectHandler

class ReadInputHandler(
    val inputChannel: InputChannel,
    val outputChannel: PrintChannel,
) : SideEffectHandler {
    override fun applies(effect: SideEffect): Boolean = effect is ReadInputEffect

    override fun handle(effect: SideEffect): String? {
        assert(effect is ReadInputEffect) {
            "Error: Expected ReadInputEffect"
        }

        val effect = effect as ReadInputEffect
        outputChannel.print(effect.prompt)
        return inputChannel.input(effect.prompt)
    }
}
