package printscript.support

import org.junit.jupiter.api.Assertions.assertEquals
import printscript.DefaultLexerFactory
import printscript.Lexer
import printscript.ast.Location
import printscript.domain.LanguageConfig
import printscript.domain.Token
import printscript.error.Error
import printscript.error.LexerError
import printscript.reader.CharPosition
import printscript.util.Result

fun lexer(
    source: String,
    config: LanguageConfig = PrintScriptLanguage.config(),
): Lexer = DefaultLexerFactory.create(MockReader(source), config)

fun lex(
    source: String,
    config: LanguageConfig = PrintScriptLanguage.config(),
): Result<List<Token>, LexerError> {
    val stream = lexer(source, config)
    val tokens = mutableListOf<Token>()
    while (true) {
        val tokenResult = stream.nextToken()
        val token =
            when (tokenResult) {
                is Result.Err -> return tokenResult
                is Result.Ok -> tokenResult.value
            }
        tokens += token
        if (token.type == "EOF") return Result.Ok(tokens)
    }
}

fun types(
    source: String,
    config: LanguageConfig = PrintScriptLanguage.config(),
): List<String> = castTokenListResult(lex(source, config)).map { it.type }

data class ExpectedToken(
    val type: String,
    val value: String? = null,
)

fun tok(
    type: String,
    value: String? = null,
): ExpectedToken = ExpectedToken(type, value)

fun assertTypes(
    source: String,
    vararg expected: String,
) {
    assertEquals(expected.toList(), types(source), "types for `$source`")
}

fun assertTypes(
    source: String,
    config: LanguageConfig,
    vararg expected: String,
) {
    assertEquals(expected.toList(), types(source, config), "types for `$source`")
}

fun assertLex(
    source: String,
    vararg expected: ExpectedToken,
) {
    assertLexed(source, castTokenListResult(lex(source)), expected)
}

fun assertLex(
    source: String,
    config: LanguageConfig,
    vararg expected: ExpectedToken,
) {
    assertLexed(source, castTokenListResult(lex(source, config)), expected)
}

fun assertLocation(
    token: Token,
    startCol: Int,
    endCol: Int,
    line: Int = 0,
) {
    assertEquals(
        Location(CharPosition(line, startCol), CharPosition(line, endCol)),
        token.location,
        "${token.type} location",
    )
}

private fun assertLexed(
    source: String,
    actual: List<Token>,
    expected: Array<out ExpectedToken>,
) {
    assertEquals(
        expected.map { it.type },
        actual.map { it.type },
        "types for `$source`",
    )
    assertEquals(
        expected.map { it.value },
        actual.map { it.value.orElse(null) },
        "values for `$source`",
    )
}

fun castTokenResult(tokenResult: Result<Token, Error>): Token = (tokenResult as Result.Ok<Token>).value

fun castTokenListResult(tokenList: Result<List<Token>, Error>): List<Token> =
    (tokenList as Result.Ok<List<Token>>).value
