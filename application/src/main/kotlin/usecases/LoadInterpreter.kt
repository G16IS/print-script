package usecases

import printscript.DefaultInterpreterFactory
import printscript.Interpreter
import printscript.error.LanguageVersionNotFound
import printscript.error.RuntimeError
import printscript.util.Result

object LoadInterpreter {
    fun load(version: String): Result<Interpreter, RuntimeError> {
        return when (version) {
            "1.0" -> Result.Ok(DefaultInterpreterFactory.create())
            "1.1" -> Result.Ok(TODO("Return interpreter version 1.1"))
            else -> return Result.Err(LanguageVersionNotFound(version))
        }
    }
}
