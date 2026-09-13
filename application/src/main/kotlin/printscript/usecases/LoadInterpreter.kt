package printscript.usecases

import printscript.DefaultInterpreterFactory
import printscript.Interpreter
import printscript.edition.LanguageCatalog
import printscript.error.RuntimeError
import printscript.util.Result
import printscript.util.map

object LoadInterpreter {
    fun load(version: String): Result<Interpreter, RuntimeError> =
        LanguageCatalog.of(version).map { kit ->
            DefaultInterpreterFactory.create(kit.evaluators, kit.executors)
        }
}
