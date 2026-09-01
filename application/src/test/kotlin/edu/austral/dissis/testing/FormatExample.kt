package edu.austral.dissis.testing

import java.io.File
import java.io.InputStream
import printscript.domain.Grammar
import printscript.error.FormatError
import printscript.infrastructure.reader.JSONGrammarConfigReader
import printscript.util.Report
import printscript.util.Result
import usecases.CheckFormat
import usecases.FormatCode

object FormatExample {
    private val language = PrintScriptLanguage.config()

    private val grammar: Grammar =
        JSONGrammarConfigReader.read(stream("grammar.config.json"))

    fun format(example: String): Result<String, FormatError> =
        FormatCode.formatCode(language, grammar, file("examples/$example"))

    fun check(example: String): Report<Unit, FormatError> =
        CheckFormat.checkFormat(language, grammar, file("examples/$example"))

    private fun stream(name: String): InputStream =
        requireNotNull(loader().getResourceAsStream(name)) { "Missing resource $name" }

    private fun file(name: String): String {
        val url = requireNotNull(loader().getResource(name)) { "Missing resource $name" }

        return File(url.toURI()).absolutePath
    }

    private fun loader(): ClassLoader = Thread.currentThread().contextClassLoader
}
