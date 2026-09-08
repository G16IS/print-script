package printscript

import printscript.util.Result
import printscript.util.flatMap
import printscript.util.map

internal fun <A, B, E> Result<A, E>.zip(other: Result<B, E>): Result<Pair<A, B>, E> =
    flatMap { a -> other.map { b -> a to b } }
