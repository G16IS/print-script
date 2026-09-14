package printscript

sealed interface SideEffect

data class PrintEffect(
    val text: String,
) : SideEffect

data class ReadInputEffect(
    val prompt: String,
) : SideEffect
