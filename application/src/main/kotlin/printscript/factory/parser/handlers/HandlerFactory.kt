package printscript.factory.parser.handlers

import printscript.parse.RuleHandler

interface HandlerFactory {
    fun acceptsVersion(version: String): Boolean

    fun getHandlers(): List<RuleHandler>
}
