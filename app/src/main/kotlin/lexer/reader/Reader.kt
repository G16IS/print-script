package lexer.reader

import token.Position

interface Reader {
    fun read(): Int
    fun getCurrentPosition(): Position
}
