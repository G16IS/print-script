package printscript.tools

import printscript.validation.ValidationIssue

/** Resultado de [PrintScriptTools.lint]. Usa solo tipos básicos. */
sealed interface LintOutcome {
    /** La versión pedida no existe o no tiene el formato `major.minor`. */
    data class UnsupportedVersion(
        val version: String,
    ) : LintOutcome

    /** Las reglas de linteo enviadas no son válidas (JSON mal formado o regla desconocida). */
    data class InvalidRules(
        val message: String,
    ) : LintOutcome

    /** El código se analizó. Si [issues] está vacío, no rompe ninguna regla. */
    data class Checked(
        val issues: List<ValidationIssue>,
    ) : LintOutcome {
        val isClean: Boolean get() = issues.isEmpty()
    }
}

/** Resultado de [PrintScriptTools.format]. Usa solo tipos básicos. */
sealed interface FormatOutcome {
    /** La versión pedida no existe o no tiene el formato `major.minor`. */
    data class UnsupportedVersion(
        val version: String,
    ) : FormatOutcome

    /** Las reglas de formato enviadas no son válidas (JSON mal formado, regla o parámetros desconocidos). */
    data class InvalidRules(
        val message: String,
    ) : FormatOutcome

    /** El código se formateó: [code] es el texto resultante. */
    data class Formatted(
        val code: String,
    ) : FormatOutcome

    /** El código no se pudo formatear (por ejemplo, no parsea). */
    data class Rejected(
        val issues: List<ValidationIssue>,
    ) : FormatOutcome
}
