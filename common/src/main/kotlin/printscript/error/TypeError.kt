package printscript.error

import printscript.ast.Location

sealed interface TypeError : Error {
    val message: String
    val location: Location
}

data class UnknownType(
    val typeName: String,
    override val location: Location,
) : TypeError {
    override val message: String
        get() = "Tipo desconocido '$typeName'"
}

data class TypeMismatch(
    val expected: String,
    val actual: String,
    override val location: Location,
) : TypeError {
    override val message: String
        get() = "Se esperaba $expected pero se encontró $actual"
}

data class Redeclaration(
    val name: String,
    override val location: Location,
) : TypeError {
    override val message: String
        get() = "La variable '$name' ya fue declarada"
}

data class UndeclaredIdentifier(
    val name: String,
    override val location: Location,
) : TypeError,
    RuntimeError {
    override val message: String
        get() = "Variable '$name' no declarada"
}

data class InvalidOperands(
    val operator: String,
    val left: String,
    val right: String,
    override val location: Location,
) : TypeError,
    RuntimeError {
    override val message: String
        get() = "El operador '$operator' no acepta $left y $right"
}

data class UnrecognizedNode(
    val nodeName: String,
    override val location: Location,
) : TypeError,
    RuntimeError {
    override val message: String
        get() = "Nodo no reconocido '$nodeName'"
}

data class TypeErrorWithMessage(
    override val message: String,
    override val location: Location,
) : TypeError
