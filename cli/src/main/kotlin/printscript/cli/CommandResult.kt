package printscript.cli

sealed interface CommandResult {
    data object Ok : CommandResult

    data class Output(
        val text: String,
    ) : CommandResult

    data class Failed(
        val messages: List<String>,
    ) : CommandResult
}
