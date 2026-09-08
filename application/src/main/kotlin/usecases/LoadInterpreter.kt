package usecases

import printscript.DefaultInterpreterFactory
import printscript.Interpreter

object LoadInterpreter {
    fun load(): Interpreter = DefaultInterpreterFactory.create()
}
