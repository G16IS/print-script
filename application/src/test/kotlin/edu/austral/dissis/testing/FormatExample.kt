package edu.austral.dissis.testing

import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import printscript.domain.FormatterRulesConfig
import printscript.domain.Grammar
import printscript.edition.LanguageCatalog
import printscript.error.Error
import printscript.formatter.Formatter
import printscript.io.DefaultSideEffectManager
import printscript.reader.FileCodeReader
import printscript.reader.JSONFormatterLanguageConfigReader
import printscript.reader.JSONFormatterRulesConfigReader
import printscript.reader.JSONGrammarConfigReader
import printscript.usecases.CheckFormat
import printscript.usecases.FormatCode
import printscript.usecases.LoadFormatter
import printscript.util.Report
import printscript.util.Result

object FormatExample {
    private val language = PrintScriptLanguage.config()

    private val grammar: Grammar =
        JSONGrammarConfigReader
            .read(stream("grammar.config.v1.0.json"))

    private val formatter: Formatter = loadFormatter()

    fun format(example: String): Result<String, Error> {
        val path = file("examples/$example")
        return FormatCode.formatCode(
            language,
            grammar,
            FileCodeReader(path),
            formatter,
            LanguageCatalog.v10(DefaultSideEffectManager()),
        )
    }

    fun check(example: String): Report<Unit, Error> {
        val path = file("examples/$example")
        val source = Files.readString(Path.of(path))
        return CheckFormat.checkFormat(
            language,
            grammar,
            FileCodeReader(path),
            source,
            formatter,
            LanguageCatalog.v10(DefaultSideEffectManager()),
        )
    }

    private fun loadFormatter(): Formatter {
        val languageConfig =
            JSONFormatterLanguageConfigReader.read(stream("formatter-language.v1.0.json"))
        val defaults =
            JSONFormatterRulesConfigReader.read(stream("formatter-user-defaults.json"))
        val loaded =
            LoadFormatter.load(
                grammar,
                language,
                languageConfig,
                FormatterRulesConfig(),
                defaults,
            )
        check(loaded is Result.Ok) { "Could not load formatter for tests: $loaded" }
        return loaded.value
    }

    private fun stream(name: String): InputStream =
        requireNotNull(loader().getResourceAsStream(name)) { "Missing resource $name" }

    private fun file(name: String): String {
        val url = requireNotNull(loader().getResource(name)) { "Missing resource $name" }

        return File(url.toURI()).absolutePath
    }

    private fun loader(): ClassLoader = Thread.currentThread().contextClassLoader
}
