package printscript
import printscript.ast.VariableType

class SymbolTable {
    private val types = mutableMapOf<String, VariableType>()

    fun isDeclared(name: String): Boolean =
        types.containsKey(name)

    fun declare(name: String, type: VariableType): Boolean {
        if (isDeclared(name)) return false
        types[name] = type
        return true
    }

    fun typeOf(name: String): VariableType? =
        types[name]
}