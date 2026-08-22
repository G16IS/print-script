package printscript.ast

enum class VariableType {
    NUMBER,
    STRING,
    ;

    companion object {
        fun from(value: String): VariableType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown variable type: '$value'")
    }
}
