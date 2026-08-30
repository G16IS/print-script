package printscript.error

import printscript.ast.Location

sealed interface LintError {
    val message: String
    val location: Location
}

data class InvalidIdentifierFormat(
    val identifier: String,
    val expectedFormat: String,
    override val location: Location,
) : LintError {
    override val message: String
        get() = "El identificador '$identifier' no respeta el formato $expectedFormat"
}

data class InvalidPrintlnArgument(
    override val location: Location,
) : LintError {
    override val message: String
        get() = "La llamada a println solo acepta un identificador o un literal"
}
