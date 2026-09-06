package printscript.infrastructure.cli

import java.nio.file.Files
import java.nio.file.Path
import printscript.cli.SourceFiles
import printscript.infrastructure.reader.FileCodeReader
import printscript.reader.CodeReader

object FileSources : SourceFiles {
    override fun reader(path: String): CodeReader = FileCodeReader(path)

    override fun text(path: String): String = Files.readString(Path.of(path))
}
