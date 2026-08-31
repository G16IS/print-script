package printscript.support

import org.junit.jupiter.api.Assertions.assertEquals
import printscript.DefaultLexerFactory
import printscript.Lexer
import printscript.ast.Location
import printscript.domain.LanguageConfig
import printscript.domain.Token
import printscript.reader.CharPosition

fun lexer(
    source: String,
    config: LanguageConfig = PrintScriptLanguage.config(),
): Lexer = DefaultLexerFactory.create(MockReader(source), config)

fun lex(
    source: String,
    config: LanguageConfig = PrintScriptLanguage.config(),
): List<Token> {
    val stream = lexer(source, config)
    val tokens = mutableListOf<Token>()
    while (true) {
        val token = stream.nextToken()
        tokens += token
        if (token.type == "EOF") return tokens
    }
}

fun types(
    source: String,
    config: LanguageConfig = PrintScriptLanguage.config(),
): List<String> = lex(source, config).map { it.type }

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
    assertLexed(source, lex(source), expected)
}

fun assertLex(
    source: String,
    config: LanguageConfig,
    vararg expected: ExpectedToken,
) {
    assertLexed(source, lex(source, config), expected)
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
