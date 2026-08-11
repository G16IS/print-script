package printscript

import printscript.common.ast.Location
import printscript.common.domain.Token
import printscript.common.domain.TokenType
import printscript.common.reader.CodeReader
import printscript.TokenStream
import java.util.Optional

object DefaultLexerFactory {
    fun create(codeReader: CodeReader): Lexer = TokenStream(codeReader, TokenRegistry(createHardCodedMap()))
}

// leerias aca las keywords
fun createHardCodedMap(): Map<String, (Location) -> Token> {
    return mapOf(
        "println" to { location: Location ->
            Token(TokenType.CALL, Optional.of("println"), location.start, location.end)
        },
        "let" to { location: Location ->
            Token(TokenType.LET, Optional.empty(), location.start, location.end)
        }
    )
}
