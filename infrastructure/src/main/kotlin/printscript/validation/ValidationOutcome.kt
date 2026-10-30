package printscript.validation

/**
 * Resultado de [PrintScriptValidation.validate]. Usa solo tipos básicos para que quien lo consuma
 * no dependa de los módulos internos de la librería.
 */
sealed interface ValidationOutcome {
    /** La versión pedida no existe o no tiene el formato `major.minor`. */
    data class UnsupportedVersion(
        val version: String,
    ) : ValidationOutcome

    /** El código se analizó. Si [issues] está vacío, es válido para la versión pedida. */
    data class Checked(
        val issues: List<ValidationIssue>,
    ) : ValidationOutcome {
        val isValid: Boolean get() = issues.isEmpty()
    }
}

/**
 * Un problema encontrado en el código.
 *
 * @property code identificador estable del tipo de error (por ejemplo `type-mismatch`).
 * @property line línea donde empieza el error, desde 1.
 * @property column columna donde empieza el error, desde 1.
 */
data class ValidationIssue(
    val code: String,
    val message: String,
    val line: Int,
    val column: Int,
)
