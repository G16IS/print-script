package printscript.tools

import printscript.definitions.EnvChannel
import printscript.definitions.PrintChannel
import printscript.definitions.ReadInputEffect
import printscript.definitions.SideEffect
import printscript.definitions.SideEffectHandler
import printscript.definitions.SideEffectManager
import printscript.domain.FormatterRulesConfig
import printscript.edition.LanguageCatalog
import printscript.edition.LanguageKit
import printscript.error.formatError
import printscript.factory.DefaultLinterFactory
import printscript.io.DefaultSideEffectManager
import printscript.io.PrintHandler
import printscript.io.ReadEnvHandler
import printscript.reader.JSONFormatterRulesConfigReader
import printscript.reader.JSONLinterConfigReader
import printscript.reader.StringCodeReader
import printscript.tck.PrintScriptConfigsLoader
import printscript.usecases.ExecuteCode
import printscript.usecases.FormatCode
import printscript.usecases.LintProgram
import printscript.util.Result
import printscript.validation.toIssue

/**
 * Entrada de la librería para linteo, formateo y ejecución con reglas que manda quien llama, en cada pedido.
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

    /**
     * Ejecuta [source] con efectos aislados: lo que imprime se junta en memoria, [inputs] alimenta a
     * `readInput` y [env] a `readEnv`. Nunca toca la consola, el ambiente real ni `.printscript/formatter.yml`.
     */
    fun execute(
        version: String,
        source: String,
        inputs: List<String>,
        env: Map<String, String>,
    ): ExecutionOutcome {
        val outputs = mutableListOf<String>()
        val out =
            object : PrintChannel {
                override fun print(text: String) {
                    outputs.add(text)
                }
            }
        val pending = ArrayDeque(inputs)
        val readInput =
            object : SideEffectHandler {
                override fun applies(effect: SideEffect) = effect is ReadInputEffect

                override fun handle(effect: SideEffect): String? {
                    out.print((effect as ReadInputEffect).prompt)
                    return pending.removeFirstOrNull()
                }
            }
        val envChannel =
            object : EnvChannel {
                override fun read(name: String): String? = env[name]
            }
        val manager: SideEffectManager =
            DefaultSideEffectManager(listOf(PrintHandler(out), readInput, ReadEnvHandler(envChannel)))
        val kit =
            when (val found = LanguageCatalog.of(version, manager)) {
                is Result.Err -> return ExecutionOutcome.UnsupportedVersion(version)
                is Result.Ok -> found.value
            }
        val configs = PrintScriptConfigsLoader.load(version, FormatterRulesConfig())
        val result =
            ExecuteCode.execute(
                configs.lang,
                configs.grammar,
                configs.typeSystem,
                StringCodeReader(source),
                kit,
            )
        return ExecutionOutcome.Finished(outputs, (result as? Result.Err)?.let { formatError(it.error) })
    }

    // Sin handlers: lint y format nunca ejecutan. Va antes de cargar configs: una versión desconocida
    // haría fallar al loader con `Missing resource`.
    private fun kitOf(version: String): LanguageKit? =
        when (val found = LanguageCatalog.of(version, DefaultSideEffectManager(emptyList()))) {
            is Result.Err -> null
            is Result.Ok -> found.value
        }
}
