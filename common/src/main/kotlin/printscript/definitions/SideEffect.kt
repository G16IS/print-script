package printscript.definitions

sealed interface SideEffect

data class PrintEffect(
    val text: String,
) : SideEffect

data class ReadInputEffect(
    val prompt: String,
) : SideEffect

data class ReadEnvEffect(
    val name: String,
) : SideEffect
