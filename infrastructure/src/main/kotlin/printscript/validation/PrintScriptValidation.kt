package printscript.validation

import java.io.InputStream
import printscript.edition.LanguageCatalog
import printscript.error.Error
import printscript.error.InvalidOperands
import printscript.error.LexerError
import printscript.error.MissingToken
import printscript.error.MultipleRulesWithSamePriority
import printscript.error.NoRulesProvided
import printscript.error.ParserError
import printscript.error.Redeclaration
import printscript.error.RuleNotFound
import printscript.error.TypeError
import printscript.error.TypeErrorWithMessage
import printscript.error.TypeMismatch
import printscript.error.UndeclaredIdentifier
import printscript.error.UnexpectedEnfOfLine
import printscript.error.UnexpectedStart
import printscript.error.UnexpectedToken
import printscript.error.UnknownType
import printscript.error.UnrecognizedNode
import printscript.io.DefaultSideEffectManager
import printscript.reader.JSONGrammarConfigReader
import printscript.reader.JSONLanguageConfigReader
import printscript.reader.JSONTypeSystemConfigReader
import printscript.reader.StringCodeReader
import printscript.syntax.Location
import printscript.usecases.TypecheckCode
import printscript.util.Result

/**
 * Entrada de la librería para chequear código sin ejecutarlo: dada una versión y el texto, dice si
 * parsea y pasa el type-checker, y si no, qué error es y dónde está.
 *
 * Solo analiza: no corre el programa ni lee archivos del directorio de trabajo.
 *
 * Los errores de **configuración** del lenguaje (no del código) no se reportan como [ValidationIssue]:
 * lanzan [IllegalStateException], porque no son culpa de quien escribió el código.
 */
object PrintScriptValidation {
    fun validate(
        version: String,
        source: String,
    ): ValidationOutcome {
        // Sin handlers: validar nunca ejecuta, así que ningún efecto debería llegar al manager.
        val kit =
            when (val found = LanguageCatalog.of(version, DefaultSideEffectManager(emptyList()))) {
                is Result.Err -> return ValidationOutcome.UnsupportedVersion(version)
                is Result.Ok -> found.value
            }

        val suffix = kit.resourceSuffix
        val report =
            TypecheckCode.check(
                resource("language.config.v$suffix.json").use { JSONLanguageConfigReader.read(it) },
                resource("grammar.config.v$suffix.json").use { JSONGrammarConfigReader.read(it) },
                resource("type-system.config.v$suffix.json").use { JSONTypeSystemConfigReader.read(it) },
                StringCodeReader(source),
                kit,
            )

        return ValidationOutcome.Checked(report.errors.map { it.toIssue() })
    }

    private fun resource(name: String): InputStream =
        requireNotNull(JSONLanguageConfigReader::class.java.classLoader.getResourceAsStream(name)) {
            "Missing resource $name"
        }
}

/**
 * Los códigos son parte del contrato con quien consume la librería: cambiarlos rompe a los clientes.
 * Solo los errores léxicos, de parseo y de tipos pueden salir de un chequeo; cualquier otro es un bug.
 */
internal fun Error.toIssue(): ValidationIssue =
    when (this) {
        is LexerError -> issue(lexerCode(), message, location)
        is ParserError -> issue(parserCode(), message, location)
        is TypeError -> issue(typeCode(), message, location)
        else -> error("Unexpected ${this::class.simpleName} while validating: $message")
    }

private fun issue(
    code: String,
    message: String,
    location: Location,
): ValidationIssue {
    val start = location.start
    check(start.line >= 1 && start.col >= 1) { "Error '$code' has no position: $message" }
    return ValidationIssue(code, message, start.line, start.col)
}

private fun LexerError.lexerCode(): String =
    when (this) {
        is UnexpectedToken -> "unexpected-token"
        is UnexpectedEnfOfLine -> "unexpected-end-of-file"
        is MultipleRulesWithSamePriority, is NoRulesProvided, is RuleNotFound ->
            error("Broken language configuration: $message")
    }

private fun ParserError.parserCode(): String =
    when (this) {
        is MissingToken -> "missing-token"
        is UnexpectedStart -> "unexpected-start"
    }

private fun TypeError.typeCode(): String =
    when (this) {
        is UnknownType -> "unknown-type"
        is TypeMismatch -> "type-mismatch"
        is Redeclaration -> "redeclaration"
        is UndeclaredIdentifier -> "undeclared-identifier"
        is InvalidOperands -> "invalid-operands"
        is UnrecognizedNode -> "unrecognized-node"
        is TypeErrorWithMessage -> "type-error"
    }
