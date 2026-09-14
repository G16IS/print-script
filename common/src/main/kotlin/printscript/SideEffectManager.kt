package printscript

interface SideEffectManager {
    fun handle(effect: SideEffect): String?
}
