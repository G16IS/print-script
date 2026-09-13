package printscript.io

import printscript.PrintEffect
import printscript.SideEffect

class ConsoleWriterHandler : SideEffectHandler {
    override fun applies(effect: SideEffect) = effect is PrintEffect

    override fun handle(effect: SideEffect): String? {
        assert(effect is PrintEffect) {
            "Error: Expected PrintEffect"
        }

        val effect = effect as PrintEffect

        println(effect.text)
        return null
    }
}
