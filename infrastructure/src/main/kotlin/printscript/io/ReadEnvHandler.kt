package printscript.io

import printscript.definitions.EnvChannel
import printscript.definitions.ReadEnvEffect
import printscript.definitions.SideEffect
import printscript.definitions.SideEffectHandler
import printscript.io.channel.SystemEnvChannel

class ReadEnvHandler(
    val envChannel: EnvChannel = SystemEnvChannel(),
) : SideEffectHandler {
    override fun applies(effect: SideEffect): Boolean = effect is ReadEnvEffect

    override fun handle(effect: SideEffect): String? {
        require(effect is ReadEnvEffect) { "Error: Expected ReadEnvEffect" }

        return envChannel.read(effect.name)
    }
}
