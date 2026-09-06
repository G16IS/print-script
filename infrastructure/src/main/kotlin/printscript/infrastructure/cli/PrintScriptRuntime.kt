package printscript.infrastructure.cli

import printscript.cli.PrintScriptCli

object PrintScriptRuntime {
    fun create(
        configFactory: ConfigFactory = DefaultConfigFactory(),
        commandFactories: List<CommandFactory> = CommandFactories.defaults(),
    ): PrintScriptCli {
        val configs = configFactory.load()
        return PrintScriptCli(
            *commandFactories.map { it.create(configs) }.toTypedArray(),
        )
    }
}
