package printscript

import printscript.ast.Location
import printscript.domain.Token
import printscript.domain.TokenType
import printscript.reader.CodeReader
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
        },
        "string" to { location: Location ->
            Token(TokenType.TYPE, Optional.of("string"), location.start, location.end)
        },
        "number" to { location: Location ->
            Token(TokenType.TYPE, Optional.of("number"), location.start, location.end)
        },
    )
}
