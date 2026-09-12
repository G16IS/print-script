package printscript.io

import printscript.SideEffect

interface SideEffectHandler {
    fun applies(effect: SideEffect): Boolean

    fun handle(effect: SideEffect): String?
}
