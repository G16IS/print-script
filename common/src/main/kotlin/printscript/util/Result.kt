package printscript.util

import printscript.error.Error
import printscript.error.TypeErrorWithMessage
import printscript.reader.CharPosition
import printscript.syntax.Location

fun <E> err(error: E) = Result.Err<E>(error)

fun <T> ok(payload: T) = Result.Ok<T>(payload)

sealed interface Result<out T, out E> {
    data class Ok<T>(
        val value: T,
    ) : Result<T, Nothing>

    data class Err<E>(
        val error: E,
    ) : Result<Nothing, E>
}

// TODO: refactor to be sealed interface
data class Report<T, out E>(
    val value: T? = null,
    val errors: List<E> = emptyList(),
) {
    val isOk: Boolean get() = errors.isEmpty()
}

fun <T, E> Result<T, E>.toReport(): Report<T, E> =
    when (this) {
        is Result.Ok -> Report(value = value, errors = emptyList())
        is Result.Err -> Report(value = null, errors = listOf(error))
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

fun <T, E, F> Result<T, E>.mapError(transform: (E) -> F): Result<T, F> =
    when (this) {
        is Result.Ok -> Result.Ok(value)
        is Result.Err -> Result.Err(transform(error))
    }

fun <T, E> Result<T, E>.unwrap(msg: String = "Called unwrap() on Result.Err"): T =
    when (this) {
        is Result.Ok -> value
        is Result.Err -> throw IllegalStateException(msg)
    }

fun <T, E> Result<T, E>.unwrapErr(msg: String = "Called unwrapErr() on Result.Ok"): E =
    when (this) {
        is Result.Ok -> throw IllegalStateException(msg)
        is Result.Err -> error
    }

/**
 * ## Catches OOM exceptions.
 * Executes the given block and returns a [Result] with the result or an error if an exception occurs.
 *
 * @param block The block of code to execute.
 * @return A [Result] containing the result of the block or an error if an exception occurs.
 */
@Suppress("TooGenericExceptionCaught")
inline fun <T> safe(block: () -> Result<T, Error>): Result<T, Error> =
    try {
        block()
    } catch (_: OutOfMemoryError) {
        err(
            TypeErrorWithMessage(
                "Java heap space",
                Location(CharPosition(0, 0), CharPosition(0, 0)),
            ),
        )
    } catch (e: Exception) {
        err(
            TypeErrorWithMessage(
                "Error inesperado: ${e.message}",
                Location(CharPosition(0, 0), CharPosition(0, 0)),
            ),
        )
    }
