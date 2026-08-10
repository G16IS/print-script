package printscript.factory

import printscript.common.ast.Location
import printscript.common.reader.CodeReader
import printscript.lexer.Call
import printscript.lexer.Let
import printscript.lexer.Lexer
import printscript.lexer.Token
import printscript.lexer.TokenStream
import printscript.tokenregistry.TokenRegistry
import java.util.Optional

object DefaultLexerFactory {
    fun create(codeReader: CodeReader): Lexer = TokenStream(codeReader, TokenRegistry(createHardCodedMap()))
}

//leerias aca las keywords
fun createHardCodedMap(): Map<String, (Location) -> Token>{
    return mapOf(
        "println" to { location: Location ->
            Token(Call(), Optional.of("println"), location.start, location.end)},

        "let" to { location: Location ->
            Token(Let(), Optional.empty(), location.start, location.end)}
    )
}
