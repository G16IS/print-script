package lexer.reader

import token.Position
import java.io.File

class FileReader(path: String): Reader {
    val realReader = File(path).bufferedReader()
    var line: Int = 1
    var col: Int = 1
    override fun read(): Int {
        val current = realReader.read()
        val char = current.toChar()
        if (char == '\n'){
            line++
            col = 1
        }else{
            col++
        }

        return current
    }

    override fun getCurrentPosition(): Position {
        return Position(line, col)
    }

}
