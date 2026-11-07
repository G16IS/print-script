package printscript.reader

/** [CodeReader] in-memory; las posiciones coinciden con [FileCodeReader] (1-based). */
class StringCodeReader(
    source: String,
) : CodeReader by ReaderCodeReader(source.reader())
