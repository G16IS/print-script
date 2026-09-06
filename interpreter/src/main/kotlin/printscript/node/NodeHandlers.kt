package printscript.node

internal fun <T> List<T>.associateByNodeNames(names: (T) -> Set<String>): Map<String, T> =
    flatMap { handler -> names(handler).map { it to handler } }.toMap()
