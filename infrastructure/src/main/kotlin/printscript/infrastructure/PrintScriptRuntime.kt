package printscript.infrastructure

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import printscript.PrintEffect
import printscript.cli.CommandResult
import printscript.cli.FileCommand
import printscript.cli.PrintScriptCli
import printscript.domain.FormatterRulesConfig
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.LinterConfig
import printscript.domain.TypeSystemConfig
import printscript.formatter.Formatter
import printscript.infrastructure.reader.FileCodeReader
import printscript.infrastructure.reader.JSONFormatterLanguageConfigReader
import printscript.infrastructure.reader.JSONFormatterRulesConfigReader
import printscript.infrastructure.reader.JSONGrammarConfigReader
import printscript.infrastructure.reader.JSONLanguageConfigReader
import printscript.infrastructure.reader.JSONLinterConfigReader
import printscript.infrastructure.reader.JSONTypeSystemConfigReader
import printscript.infrastructure.reader.YAMLFormatterRulesConfigReader
import printscript.util.fold
import usecases.CheckFormat
import usecases.ExecuteCode
import usecases.ExecutionFailure
import usecases.FormatCode
import usecases.InterpretCode
import usecases.LintProgram
import usecases.LoadFormatter

object PrintScriptRuntime {
    const val USER_YAML_PATH = ".printscript/formatter.yml"

    fun create(): PrintScriptCli {
        val lang = JSONLanguageConfigReader.read(resource("language.config.json"))
        val grammar = JSONGrammarConfigReader.read(resource("grammar.config.json"))
        val typeSystem = JSONTypeSystemConfigReader.read(resource("type-system.config.json"))
        val linterConfig = JSONLinterConfigReader.read(resource("linter.config.json"))
        val formatter = loadFormatter(lang, grammar)

        return PrintScriptCli(
            run = FileCommand { path -> catching { execute(path, lang, grammar, typeSystem) } },
            lint = FileCommand { path -> catching { lint(path, lang, grammar, linterConfig) } },
            check = FileCommand { path -> catching { check(path, lang, grammar, formatter) } },
            format = FileCommand { path -> catching { format(path, lang, grammar, formatter) } },
            typecheck = FileCommand { path -> catching { typecheck(path, lang, grammar, typeSystem) } },
        )
    }

    private fun execute(
        path: String,
        lang: LanguageConfig,
        grammar: Grammar,
        typeSystem: TypeSystemConfig,
    ): CommandResult =
        ExecuteCode
            .execute(lang, grammar, typeSystem, FileCodeReader(path))
            .fold(
                onOk = { effects ->
                    CommandResult.Output(
                        effects.joinToString("") { effect ->
                            when (effect) {
                                is PrintEffect -> effect.text + "\n"
                            }
                        },
                    )
                },
                onErr = { failure ->
                    when (failure) {
                        is ExecutionFailure.Types ->
                            CommandResult.Failed(failure.errors.map { formatTypeError(it) })
                        is ExecutionFailure.Runtime ->
                            CommandResult.Failed(listOf(formatRuntimeError(failure.error)))
                    }
                },
            )

    private fun typecheck(
        path: String,
        lang: LanguageConfig,
        grammar: Grammar,
        typeSystem: TypeSystemConfig,
    ): CommandResult {
        val report =
            InterpretCode.interpretCode(lang, grammar, typeSystem, FileCodeReader(path))
        return if (report.isOk) {
            CommandResult.Ok
        } else {
            CommandResult.Failed(report.errors.map { formatTypeError(it) })
        }
    }

    private fun lint(
        path: String,
        lang: LanguageConfig,
        grammar: Grammar,
        linterConfig: LinterConfig,
    ): CommandResult {
        val report = LintProgram.lint(lang, grammar, FileCodeReader(path), linterConfig)
        return if (report.isOk) {
            CommandResult.Ok
        } else {
            CommandResult.Failed(report.errors.map { formatLintError(it) })
        }
    }

    private fun check(
        path: String,
        lang: LanguageConfig,
        grammar: Grammar,
        formatter: Formatter,
    ): CommandResult {
        val source = Files.readString(Path.of(path))
        val report =
            CheckFormat.checkFormat(lang, grammar, FileCodeReader(path), source, formatter)
        return if (report.isOk) {
            CommandResult.Ok
        } else {
            CommandResult.Failed(report.errors.map { formatFormatError(it) })
        }
    }

    private fun format(
        path: String,
        lang: LanguageConfig,
        grammar: Grammar,
        formatter: Formatter,
    ): CommandResult =
        FormatCode
            .formatCode(lang, grammar, FileCodeReader(path), formatter)
            .fold(
                onOk = { CommandResult.Output(it) },
                onErr = { CommandResult.Failed(listOf(formatFormatError(it))) },
            )

    private fun loadFormatter(
        lang: LanguageConfig,
        grammar: Grammar,
    ): Formatter {
        val language = JSONFormatterLanguageConfigReader.read(resource("formatter-language.json"))
        val defaults = JSONFormatterRulesConfigReader.read(resource("formatter-user-defaults.json"))
        val userYaml = Path.of(USER_YAML_PATH)
        val user =
            if (Files.exists(userYaml)) {
                YAMLFormatterRulesConfigReader.read(userYaml)
            } else {
                FormatterRulesConfig()
            }

        return LoadFormatter.load(grammar, lang, language, user, defaults).fold(
            onOk = { it },
            onErr = { error("Could not load formatter: ${it.message}") },
        )
    }
}

private fun catching(block: () -> CommandResult): CommandResult =
    try {
        block()
    } catch (_: Exception) {
        CommandResult.Failed(listOf("ERROR"))
    }

private fun resource(name: String): InputStream =
    requireNotNull(PrintScriptRuntime::class.java.classLoader.getResourceAsStream(name)) {
        "Missing resource $name"
    }
