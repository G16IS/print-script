package printscript.validation

import java.io.InputStream
import java.io.InputStreamReader
import printscript.edition.LanguageCatalog
import printscript.error.Error
import printscript.error.LexerError
import printscript.error.MultipleRulesWithSamePriority
import printscript.error.NoRulesProvided
import printscript.error.ParserError
import printscript.error.RuleNotFound
import printscript.error.TypeError
import printscript.error.UnexpectedEnfOfLine
import printscript.error.UnexpectedToken
import printscript.io.DefaultSideEffectManager
import printscript.reader.CodeReader
import printscript.reader.JSONGrammarConfigReader
import printscript.reader.JSONLanguageConfigReader
import printscript.reader.JSONTypeSystemConfigReader
import printscript.reader.ReaderCodeReader
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
    ): ValidationOutcome = validateWith(version) { StringCodeReader(source) }

    /**
     * Igual que la versión con [String], pero lee el código de a poco desde [input] (UTF-8), sin cargarlo
     * entero en memoria: sirve para archivos que no caben como texto. La memoria no depende del tamaño del
     * código, y el análisis se corta en el primer error de sintaxis sin leer el resto.
     *
     * Los bytes que no son UTF-8 válido se reemplazan por `�`. Quien llama es dueño de [input]: no se
     * cierra. Si la versión no existe, [input] no se toca.
     */
    fun validate(
        version: String,
        input: InputStream,
    ): ValidationOutcome = validateWith(version) { ReaderCodeReader(InputStreamReader(input, Charsets.UTF_8)) }

    // El reader se arma recién con la versión validada: al crearse ya lee el primer carácter del origen.
    private fun validateWith(
        version: String,
        reader: () -> CodeReader,
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
                reader(),
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
 * El código distingue solo la fase que falló ([IssueCodes]); el detalle va en el mensaje. Solo los errores
 * léxicos, de parseo y de tipos pueden salir de un chequeo; cualquier otro es un bug.
 */
internal fun Error.toIssue(): ValidationIssue =
    when (this) {
        is LexerError -> syntaxIssue()
        is ParserError -> issue(IssueCodes.SYNTAX, message, location)
        is TypeError -> issue(IssueCodes.TYPE, message, location)
        else -> error("Unexpected ${this::class.simpleName} while validating: $message")
    }

private fun LexerError.syntaxIssue(): ValidationIssue =
    when (this) {
        is UnexpectedToken, is UnexpectedEnfOfLine -> issue(IssueCodes.SYNTAX, message, location)
        is MultipleRulesWithSamePriority, is NoRulesProvided, is RuleNotFound ->
            error("Broken language configuration: $message")
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
