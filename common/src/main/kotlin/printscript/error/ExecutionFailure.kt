package printscript.error

sealed interface ExecutionFailure : Error {
    data class Types(
        val errors: List<Error>,
    ) : ExecutionFailure

    data class Runtime(
        val error: RuntimeError,
    ) : ExecutionFailure
}
