package printscript.infrastructure.cli

fun interface ConfigFactory {
    fun load(): PrintScriptConfigs
}
