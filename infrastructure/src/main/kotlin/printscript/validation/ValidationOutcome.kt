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

/** Valores posibles de [ValidationIssue.code]. Son parte del contrato con quien consume la librería. */
object IssueCodes {
    /** El código no se pudo leer: error léxico o de parseo. */
    const val SYNTAX = "syntax"

    /** El código se leyó pero no pasa el type-checker. */
    const val TYPE = "type"
}

/**
 * Un problema encontrado en el código.
 *
 * @property code fase que falló: [IssueCodes.SYNTAX] o [IssueCodes.TYPE]. El detalle está en [message].
 * @property line línea donde empieza el error, desde 1.
 * @property column columna donde empieza el error, desde 1.
 */
data class ValidationIssue(
    val code: String,
    val message: String,
    val line: Int,
    val column: Int,
)
