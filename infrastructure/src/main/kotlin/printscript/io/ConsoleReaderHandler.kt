package printscript.io

import printscript.ReadInputEffect
import printscript.SideEffect

class ConsoleReaderHandler : SideEffectHandler {
    override fun applies(effect: SideEffect): Boolean = effect is ReadInputEffect

    override fun handle(effect: SideEffect): String? {
        assert(effect is ReadInputEffect) {
            "Error: Expected ReadInputEffect"
        }

        val effect = effect as ReadInputEffect

        print(effect.prompt)
        return readlnOrNull()
    }
}
