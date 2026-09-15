package printscript.io

import printscript.EnvChannel
import printscript.ReadEnvEffect
import printscript.SideEffect
import printscript.SideEffectHandler
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
