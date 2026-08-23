package printscript

sealed interface SideEffect

data class PrintEffect(val text: String): SideEffect
