package printscript.typechecker.support

import java.util.Optional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import printscript.DefaultParserFactory
import printscript.Lexer
import printscript.TokenStream.Companion.END_TOKEN
import printscript.domain.TypeSystemConfig
import printscript.error.TypeError
import printscript.parse.RuleHandlers
import printscript.reader.CharPosition
import printscript.reader.CodeReader
import printscript.reader.JSONGrammarConfigReader
import printscript.reader.JSONLanguageConfigReader
import printscript.reader.JSONTypeSystemConfigReader
import printscript.syntax.SyntaxProgram
import printscript.typechecker.DefaultKindHandlerFactory
import printscript.typechecker.ScopeStack
import printscript.typechecker.TypeChecker
import printscript.util.Result
import printscript.util.isOk

private object PrintScriptV11 {
    val language = JSONLanguageConfigReader.read(resource("language.config.v1.1.json"))
    val grammar = JSONGrammarConfigReader.read(resource("grammar.config.v1.1.json"))
    val typeSystem = JSONTypeSystemConfigReader.read(resource("type-system.config.v1.1.json"))
}

fun typeSystem(): TypeSystemConfig = PrintScriptV11.typeSystem

fun parse(source: String): SyntaxProgram {
    val lexer = Lexer.create(SourceReader(source), PrintScriptV11.language)
    val parser = DefaultParserFactory.create(PrintScriptV11.grammar, RuleHandlers.defaults())
    val builder = SyntaxProgram.builder()
    while (true) {
        when (val peeked = lexer.peek()) {
            is Result.Err -> error("lex: ${peeked.error.message}")
            is Result.Ok -> {
                if (peeked.value.type == END_TOKEN) break
                when (val parsed = parser.parseNextStatement(lexer)) {
                    is Result.Err -> error("parse: ${parsed.error.message}")
                    is Result.Ok -> builder.add(parsed.value)
                }
            }
        }
    }
    return builder.build()
}

fun checker(config: TypeSystemConfig = typeSystem()): TypeChecker =
    TypeChecker.create(config, DefaultKindHandlerFactory())

fun assertOk(source: String) {
    val result = checker().check(parse(source))
    assertTrue(result.isOk, (result as? Result.Err)?.error?.message)
}

fun assertErr(
    source: String,
    fragment: String,
) {
    val result = checker().check(parse(source))
    assertTrue(result is Result.Err, "expected an error containing \"$fragment\"")
    val message = (result as Result.Err).error.message
    assertTrue(message.contains(fragment)) { message }
}

data class Walk(
    val errors: List<TypeError>,
    val scope: ScopeStack,
)

fun walk(source: String): Walk {
    val typeChecker = checker()
    var scope = ScopeStack()
    val errors = mutableListOf<TypeError>()
    for (statement in parse(source).statements) {
        when (val checked = typeChecker.checkNode(statement, scope)) {
            is Result.Err -> {
                errors += checked.error.error
                scope = checked.error.scope
            }
            is Result.Ok -> scope = checked.value.first
        }
    }
    return Walk(errors, scope)
}

fun assertWalk(
    source: String,
    vararg fragments: String,
) {
    val walked = walk(source)
    assertEquals(fragments.size, walked.errors.size)
    fragments.forEachIndexed { index, fragment ->
        assertTrue(walked.errors[index].message.contains(fragment)) { walked.errors[index].message }
    }
}

private fun resource(name: String) =
    requireNotNull(Thread.currentThread().contextClassLoader.getResourceAsStream(name)) {
        "Missing resource $name"
    }

private class SourceReader(
    private val source: String,
) : CodeReader {
    private var index = 0

    override fun read(): Optional<Char> {
        if (index >= source.length) return Optional.empty()
        return Optional.of(source[index++])
    }

    override fun peek(): Optional<Char> = if (index >= source.length) Optional.empty() else Optional.of(source[index])

    override fun currentPosition(): CharPosition = CharPosition(1, index + 1)
}
