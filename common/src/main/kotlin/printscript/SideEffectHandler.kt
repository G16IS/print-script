package printscript

interface SideEffectHandler {
    fun applies(effect: SideEffect): Boolean

    fun handle(effect: SideEffect): String?
}
