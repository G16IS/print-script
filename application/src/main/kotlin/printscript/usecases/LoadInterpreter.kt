package printscript.usecases

import printscript.Interpreter
import printscript.error.LanguageVersionNotFound
import printscript.error.RuntimeError
import printscript.factory.InterpreterFactory
import printscript.util.Result

object LoadInterpreter {
    fun load(version: String): Result<Interpreter, RuntimeError> =
        when (version) {
            "1.0" -> InterpreterFactory.create("1")
            "1.1" -> Result.Ok(TODO("Return interpreter version 1.1"))
            else -> Result.Err(LanguageVersionNotFound(version))
        }
}
