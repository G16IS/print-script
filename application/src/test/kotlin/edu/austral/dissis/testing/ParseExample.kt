package edu.austral.dissis.testing

import java.io.File
import java.io.InputStream
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.infrastructure.reader.JSONGrammarConfigReader
import printscript.syntax.SyntaxProgram
import usecases.InterpretCode.interpretCode

object ParseExample {
    private val language: LanguageConfig = PrintScriptLanguage.config()

    private val grammar: Grammar =
        JSONGrammarConfigReader.read(stream("grammar.config.json"))

    fun parse(example: String): SyntaxProgram = interpretCode(language, grammar, file("examples/$example"))

    private fun stream(name: String): InputStream =
        requireNotNull(loader().getResourceAsStream(name)) { "Missing resource $name" }

    private fun file(name: String): String {
        val url = requireNotNull(loader().getResource(name)) { "Missing resource $name" }
        return File(url.toURI()).absolutePath
    }

    private fun loader(): ClassLoader = Thread.currentThread().contextClassLoader
}
