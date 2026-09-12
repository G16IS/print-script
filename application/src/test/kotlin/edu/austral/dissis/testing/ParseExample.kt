package edu.austral.dissis.testing

import java.io.File
import java.io.InputStream
import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.TypeSystemConfig
import printscript.error.Error
import printscript.reader.FileCodeReader
import printscript.reader.JSONGrammarConfigReader
import printscript.reader.JSONTypeSystemConfigReader
import printscript.syntax.SyntaxProgram
import printscript.util.Report
import usecases.TypecheckCode.typecheck

object ParseExample {
    private val language: LanguageConfig = PrintScriptLanguage.config()

    private val grammar: Grammar =
        JSONGrammarConfigReader.read(stream("grammar.config.v1.json"))

    private val typeSystem: TypeSystemConfig =
        JSONTypeSystemConfigReader.read(stream("type-system.config.v1.json"))

    fun parse(example: String): Report<SyntaxProgram, Error> =
        typecheck(
            language,
            grammar,
            typeSystem,
            FileCodeReader(file("examples/$example")),
        )

    private fun stream(name: String): InputStream =
        requireNotNull(loader().getResourceAsStream(name)) { "Missing resource $name" }

    private fun file(name: String): String {
        val url = requireNotNull(loader().getResource(name)) { "Missing resource $name" }
        return File(url.toURI()).absolutePath
    }

    private fun loader(): ClassLoader = Thread.currentThread().contextClassLoader
}
