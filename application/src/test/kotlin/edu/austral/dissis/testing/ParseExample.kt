package edu.austral.dissis.testing

import java.io.File
import java.io.InputStream
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.TypeSystemConfig
import printscript.infrastructure.reader.JSONGrammarConfigReader
import printscript.infrastructure.reader.JSONTypeSystemConfigReader
import printscript.syntax.SyntaxProgram
import usecases.InterpretCode.interpretCode

object ParseExample {
    private val language: LanguageConfig = PrintScriptLanguage.config()

    private val grammar: Grammar =
        JSONGrammarConfigReader.read(stream("grammar.config.json"))

    private val typeSystem: TypeSystemConfig =
        JSONTypeSystemConfigReader.read(stream("type-system.config.json"))

    fun parse(example: String): SyntaxProgram = interpretCode(language, grammar, typeSystem, file("examples/$example"))

    private fun stream(name: String): InputStream =
        requireNotNull(loader().getResourceAsStream(name)) { "Missing resource $name" }

    private fun file(name: String): String {
        val url = requireNotNull(loader().getResource(name)) { "Missing resource $name" }
        return File(url.toURI()).absolutePath
    }

    private fun loader(): ClassLoader = Thread.currentThread().contextClassLoader
}
