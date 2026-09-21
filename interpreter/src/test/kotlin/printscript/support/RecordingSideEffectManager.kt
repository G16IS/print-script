package printscript.support

import printscript.definitions.SideEffect
import printscript.definitions.SideEffectManager

class RecordingSideEffectManager : SideEffectManager {
    val effects = mutableListOf<SideEffect>()

    override fun handle(effect: SideEffect): String? {
        effects += effect
        return null
    }
}
