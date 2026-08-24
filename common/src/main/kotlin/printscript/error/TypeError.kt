package printscript.error

import printscript.ast.Location

sealed interface TypeError {
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
) : TypeError {
    override val message: String
        get() = "Variable '$name' no declarada"
}

data class InvalidOperands(
    val operator: String,
    val left: String,
    val right: String,
    override val location: Location,
) : TypeError {
    override val message: String
        get() = "El operador '$operator' no acepta $left y $right"
}

data class UnrecognizedNode(
    val nodeName: String,
    override val location: Location,
) : TypeError {
    override val message: String
        get() = "Nodo no reconocido '$nodeName'"
}

data class UnresolvableExpression(
    val nodeName: String,
    override val location: Location,
) : TypeError {
    override val message: String
        get() = "No hay evaluator para nodo '$nodeName'"
}

data class NoNodeKindForNode(
    val nodeName: String,
    override val location: Location,
) : TypeError {
    override val message: String
        get() = "El tipo de nodo '$nodeName' no está mapeado a ningún NodeKind. Revisá el config de mapping."
}

data class DivisionByZero(
    override val location: Location,
) : TypeError {
    override val message: String
        get() = "División por cero"
}

data class InvalidLiteral(
    val literal: String,
    override val location: Location,
) : TypeError {
    override val message: String
        get() = "Literal inválido '$literal'"
}

data class UnresolvableCall(
    val callee: String,
    override val location: Location,
) : TypeError {
    override val message: String
        get() = "Llamada desconocida '$callee'"
}
