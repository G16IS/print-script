package printscript.io

import printscript.SideEffect
import printscript.SideEffectHandler
import printscript.SideEffectManager

class DefaultSideEffectManager(
    val handlers: List<SideEffectHandler>,
) : SideEffectManager {
    constructor() : this(
        listOf(
            PrintHandler(),
            ReadInputHandler(),
            ReadEnvHandler(),
        ),
    )

    override fun handle(effect: SideEffect): String? {
        for (handler in handlers) {
            if (!handler.applies(effect)) continue
            return handler.handle(effect)
        }

        throw IllegalArgumentException("No handler found for effect: $effect")
    }
}
