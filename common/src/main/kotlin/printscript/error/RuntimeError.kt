package printscript.error

import printscript.ast.Location
import printscript.reader.CharPosition

sealed interface RuntimeError : Error {
    val message: String
    val location: Location
}

data class UnresolvableExpression(
    val nodeName: String,
    override val location: Location,
) : RuntimeError {
    override val message: String
        get() = "No hay handler registrado para nodo '$nodeName'"
}

data class DivisionByZero(
    override val location: Location,
) : RuntimeError {
    override val message: String
        get() = "División por cero"
}

data class InvalidLiteral(
    val literal: String,
    override val location: Location,
) : RuntimeError {
    override val message: String
        get() = "Literal inválido '$literal'"
}

data class UnresolvableCall(
    val callee: String,
    override val location: Location,
) : RuntimeError {
    override val message: String
        get() = "Llamada desconocida '$callee'"
}

data class LanguageVersionNotFound(
    val version: String,
) : RuntimeError {
    override val message: String
        get() = "La version $version de PrintScript no se encontro"
    val charPos = CharPosition(0, 0)
    override val location: Location
        get() = Location(charPos, charPos)
}
