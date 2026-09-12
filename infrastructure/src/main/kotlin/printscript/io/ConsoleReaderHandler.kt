package printscript.io

import printscript.SideEffect

class ConsoleReaderHandler : SideEffectHandler {
    override fun applies(effect: SideEffect): Boolean {
        return false // effect is ReadEffect
    }

    override fun handle(effect: SideEffect): String? {
        throw NotImplementedError("ConsoleReaderHandler is not implemented yet")
//        assert(effect is ReadEffect) { "Error: Expected ReadEffect" }
//        val effect = effect as ReadEffect

//        print(effect.prompt)
//        return readLine()
    }
}
