package printscript.formatter

import printscript.ast.Location

sealed interface FormatError {
    val message: String
    val location: Location
}

data class MissingLexeme(
    val tokenType: String,
    override val location: Location,
) : FormatError {
    override val message: String = "El token '$tokenType' no tiene lexema para emitir"
}

data class UnrecognizedNode(
    val nodeName: String,
    override val location: Location,
) : FormatError {
    override val message: String = "Nodo no reconocido '$nodeName'"
}

data class WhitespaceMismatch(
    val expected: String,
    val actual: String,
    override val location: Location,
) : FormatError {
    override val message: String =
        "Se esperaba whitespace ${visible(expected)} pero se encontró ${visible(actual)}"
}

data class UnknownRuleType(
    val type: String,
    override val location: Location = Location.empty(),
) : FormatError {
    override val message: String = "Tipo de regla de formato desconocido: '$type'"
}

data class InvalidRuleParams(
    val type: String,
    val reason: String,
    override val location: Location = Location.empty(),
) : FormatError {
    override val message: String = "Parámetros inválidos para '$type': $reason"
}

data class UserDeclaredFixedRule(
    val type: String,
    override val location: Location = Location.empty(),
) : FormatError {
    override val message: String =
        "La regla '$type' es de lenguaje y no puede declararse en el YAML de usuario"
}

private fun visible(whitespace: String): String = "\"${whitespace.replace("\n", "\\n").replace("\t", "\\t")}\""
