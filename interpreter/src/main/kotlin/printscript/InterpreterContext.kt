package printscript

class InterpreterContext private constructor(
    private val parent: InterpreterContext?,
    private val variables: Map<String, RuntimeValue>
){
}
