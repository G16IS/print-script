package edu.austral.dissis.testing

import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import printscript.domain.FormatterRulesConfig
import printscript.domain.Grammar
import printscript.error.FormatError
import printscript.formatter.Formatter
import printscript.infrastructure.reader.FileCodeReader
import printscript.infrastructure.reader.JSONFormatterLanguageConfigReader
import printscript.infrastructure.reader.JSONFormatterRulesConfigReader
import printscript.infrastructure.reader.JSONGrammarConfigReader
import printscript.util.Report
import printscript.util.Result
import usecases.CheckFormat
import usecases.FormatCode
import usecases.LoadFormatter

object FormatExample {
    private val language = PrintScriptLanguage.config()

    private val grammar: Grammar =
        JSONGrammarConfigReader.read(stream("grammar.config.json"))

    private val formatter: Formatter = loadFormatter()

    fun format(example: String): Result<String, FormatError> {
        val path = file("examples/$example")
        return FormatCode.formatCode(language, grammar, FileCodeReader(path), formatter)
    }

    fun check(example: String): Report<Unit, FormatError> {
        val path = file("examples/$example")
        val source = Files.readString(Path.of(path))
        return CheckFormat.checkFormat(language, grammar, FileCodeReader(path), source, formatter)
    }

    private fun loadFormatter(): Formatter {
        val languageConfig =
            JSONFormatterLanguageConfigReader.read(stream("formatter-language.json"))
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
