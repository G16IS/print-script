package printscript.support

import printscript.error.RuntimeError
import printscript.util.Result

fun <T> ok(result: Result<T, RuntimeError>): T = (result as Result.Ok).value

fun <T> err(result: Result<T, RuntimeError>): RuntimeError = (result as Result.Err).error
