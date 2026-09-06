package printscript.cli

import printscript.reader.CodeReader

interface SourceFiles {
    fun reader(path: String): CodeReader

    fun text(path: String): String
}

fun interface CommandEffects<T> {
    fun handle(block: () -> T): CommandResult
}
