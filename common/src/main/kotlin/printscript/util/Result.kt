package printscript.util

sealed interface Result<out T, out E> {
    data class Ok<T>(
        val value: T,
    ) : Result<T, Nothing>

    data class Err<E>(
        val error: E,
    ) : Result<Nothing, E>
}

data class Report<T, E>(
    val value: T? = null,
    val errors: List<E> = emptyList(),
) {
    val isOk: Boolean get() = errors.isEmpty()
}

val Result<*, *>.isOk: Boolean get() = this is Result.Ok

inline fun <T, E, R> Result<T, E>.map(transform: (T) -> R): Result<R, E> =
    when (this) {
        is Result.Ok -> Result.Ok(transform(value))
        is Result.Err -> this
    }

inline fun <T, E, R> Result<T, E>.flatMap(transform: (T) -> Result<R, E>): Result<R, E> =
    when (this) {
        is Result.Ok -> transform(value)
        is Result.Err -> this
    }

inline fun <T, E, R> Result<T, E>.fold(
    onOk: (T) -> R,
    onErr: (E) -> R,
): R =
    when (this) {
        is Result.Ok -> onOk(value)
        is Result.Err -> onErr(error)
    }
