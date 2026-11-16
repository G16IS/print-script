package printscript.tools

import printscript.edition.LanguageCatalog
import printscript.edition.LanguageKit
import printscript.factory.DefaultLinterFactory
import printscript.io.DefaultSideEffectManager
import printscript.reader.JSONFormatterRulesConfigReader
import printscript.reader.JSONLinterConfigReader
import printscript.reader.StringCodeReader
import printscript.tck.PrintScriptConfigsLoader
import printscript.usecases.FormatCode
import printscript.usecases.LintProgram
import printscript.util.Result
import printscript.validation.toIssue

/**
 * Entrada de la librería para linteo y formateo con reglas que manda quien llama, en cada pedido.
 * Solo usa tipos básicos y no ejecuta el código.
 *
 * Las reglas de linteo **reemplazan** a las internas (una regla que no se manda queda apagada); las de
 * formato se apoyan sobre los defaults internos.
 */
object PrintScriptTools {
    /** [rulesJson] tiene el formato de `linter.config.v1.1.json`. */
    fun lint(
        version: String,
        source: String,
        rulesJson: String,
    ): LintOutcome {
        val kit = kitOf(version) ?: return LintOutcome.UnsupportedVersion(version)
        val configs =
            try {
                PrintScriptConfigsLoader.load(version, JSONLinterConfigReader.read(rulesJson)).also {
                    // Solo valida: tira IllegalArgumentException ante una regla u opción inválida.
                    DefaultLinterFactory.create(it.linterConfig)
                }
            } catch (e: IllegalArgumentException) {
                // Incluye SerializationException (JSON mal formado).
                return LintOutcome.InvalidRules(e.message ?: "Invalid lint rules")
            }
        val report =
            LintProgram.lint(configs.lang, configs.grammar, StringCodeReader(source), configs.linterConfig, kit)
        return LintOutcome.Checked(report.errors.map { it.toIssue() })
    }

    /** [rulesJson] tiene el formato de `formatter-user-defaults.json`. */
    fun format(
        version: String,
        source: String,
        rulesJson: String,
    ): FormatOutcome {
        val kit = kitOf(version) ?: return FormatOutcome.UnsupportedVersion(version)
        val configs =
            try {
                PrintScriptConfigsLoader.load(version, JSONFormatterRulesConfigReader.read(rulesJson))
            } catch (e: IllegalArgumentException) {
                return FormatOutcome.InvalidRules(e.message ?: "Invalid format rules")
            } catch (e: IllegalStateException) {
                return FormatOutcome.InvalidRules(e.message ?: "Invalid format rules")
            }
        return when (
            val result =
                FormatCode.formatCode(configs.lang, configs.grammar, StringCodeReader(source), configs.formatter, kit)
        ) {
            is Result.Ok -> FormatOutcome.Formatted(result.value)
            is Result.Err -> FormatOutcome.Rejected(listOf(result.error.toIssue()))
        }
    }

    // Sin handlers: lint y format nunca ejecutan. Va antes de cargar configs: una versión desconocida
    // haría fallar al loader con `Missing resource`.
    private fun kitOf(version: String): LanguageKit? =
        when (val found = LanguageCatalog.of(version, DefaultSideEffectManager(emptyList()))) {
            is Result.Err -> null
            is Result.Ok -> found.value
        }
}
