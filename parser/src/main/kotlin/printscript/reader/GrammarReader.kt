package printscript.reader

import printscript.grammar.Grammar

/**
 * Hook for loading [Grammar] from `grammar.config.json`.
 * The parser module does not ship a JSON implementation (no extra deps).
 */
interface GrammarReader {
    fun read(json: String): Grammar
}
