package printscript.definitions

interface SideEffectManager {
    fun handle(effect: SideEffect): String?
}
