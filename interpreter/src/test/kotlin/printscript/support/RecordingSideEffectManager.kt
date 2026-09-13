package printscript.support

import printscript.SideEffect
import printscript.SideEffectManager

class RecordingSideEffectManager : SideEffectManager {
    val effects = mutableListOf<SideEffect>()

    override fun handle(effect: SideEffect): String? {
        effects += effect
        return null
    }
}
