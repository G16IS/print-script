package printscript.support

import printscript.domain.Grammar
import printscript.reader.JSONGrammarConfigReader

/** Loaded from `grammar.config.json` on the infrastructure classpath. */
val PrintScriptGrammar: Grammar = JSONGrammarConfigReader.read(grammarStream())

private fun grammarStream() =
    JSONGrammarConfigReader::class.java.getResourceAsStream("/grammar.config.v1.json")
        ?: error("Missing grammar.config.v1.json")
