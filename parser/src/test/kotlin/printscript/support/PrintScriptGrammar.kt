package printscript.support

import printscript.domain.Grammar
import printscript.infrastructure.reader.JSONGrammarConfigReader

/** Loaded from `grammar.config.json` on the infrastructure classpath. */
val PrintScriptGrammar: Grammar = JSONGrammarConfigReader.read(grammarStream())

private fun grammarStream() =
    JSONGrammarConfigReader::class.java.getResourceAsStream("/grammar.config.json")
        ?: error("Missing grammar.config.json")
