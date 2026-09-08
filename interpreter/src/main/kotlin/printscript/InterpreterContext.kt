package printscript

import printscript.ast.Location
import printscript.error.RuntimeError
import printscript.error.UndeclaredIdentifier
import printscript.util.Result
import printscript.util.map

/**
 * Immutable, copy-on-write environment. Every mutation returns a new context,
 * so executors can thread the latest context through the program.
 */
class InterpreterContext private constructor(
    private val parent: InterpreterContext?,
    private val variables: Map<String, RuntimeValue>,
) {
    constructor() : this(null, emptyMap())

    fun childScope(): InterpreterContext = InterpreterContext(this, emptyMap())

    fun getVariable(name: String): RuntimeValue? = variables[name] ?: parent?.getVariable(name)

    fun declareVariable(
        name: String,
        value: RuntimeValue,
    ): InterpreterContext = InterpreterContext(parent, variables + (name to value))

    fun assignVariable(
        name: String,
        value: RuntimeValue,
        location: Location = Location.empty(),
    ): Result<InterpreterContext, RuntimeError> =
        when {
            variables.containsKey(name) ->
                Result.Ok(InterpreterContext(parent, variables + (name to value)))
            parent != null ->
                parent.assignVariable(name, value, location).map { rebuiltParent ->
                    InterpreterContext(rebuiltParent, variables)
                }
            else -> Result.Err(UndeclaredIdentifier(name, location))
        }
}
