package printscript.io

import printscript.SideEffect

class SideEffectManager(
    val handlers: List<SideEffectHandler>,
) {
    constructor() : this(
        listOf(
            ConsoleWriterHandler(),
            ConsoleReaderHandler(),
        ),
    )

    fun handle(effect: SideEffect): String? {
        for (handler in handlers) {
            if (!handler.applies(effect)) continue
            return handler.handle(effect)
        }

        throw IllegalArgumentException("No handler found for effect: $effect")
    }
}
