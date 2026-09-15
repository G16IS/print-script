package printscript

import printscript.error.RuntimeError
import printscript.error.UndeclaredIdentifier
import printscript.syntax.Location
import printscript.util.Result
import printscript.util.map

/**
 * Immutable, copy-on-write environment. Every mutation returns a new context,
 * so executors can thread the latest context through the program.
 */
class InterpreterContext private constructor(
    private val parent: InterpreterContext?,
    private val variables: Map<String, RuntimeValue>,
    private val declaredTypes: Map<String, String> = emptyMap(),
) {
    constructor() : this(null, emptyMap())

    fun childScope(): InterpreterContext = InterpreterContext(this, emptyMap())

    fun getVariable(name: String): RuntimeValue? = variables[name] ?: parent?.getVariable(name)

    /**
     * Tipo con el que se declaró la variable, si se conoce. Es lo que decide a
     * qué convertir un `readInput` / `readEnv` asignado a ella.
     */
    fun typeOf(name: String): String? = declaredTypes[name] ?: parent?.typeOf(name)

    fun declareVariable(
        name: String,
        value: RuntimeValue,
        declaredType: String? = null,
    ): InterpreterContext =
        InterpreterContext(
            parent,
            variables + (name to value),
            if (declaredType == null) declaredTypes else declaredTypes + (name to declaredType),
        )

    fun assignVariable(
        name: String,
        value: RuntimeValue,
        location: Location = Location.empty(),
    ): Result<InterpreterContext, RuntimeError> =
        when {
            variables.containsKey(name) ->
                Result.Ok(InterpreterContext(parent, variables + (name to value), declaredTypes))
            parent != null ->
                parent.assignVariable(name, value, location).map { rebuiltParent ->
                    InterpreterContext(rebuiltParent, variables, declaredTypes)
                }
            else -> Result.Err(UndeclaredIdentifier(name, location))
        }
}
