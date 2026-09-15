package printscript.reader

import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.readText
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import printscript.domain.AtomRule
import printscript.domain.Grammar
import printscript.domain.GrammarRule
import printscript.domain.LeftRule
import printscript.domain.OptionalRule
import printscript.domain.OrRule
import printscript.domain.RepeatRule
import printscript.domain.SeqRule
import printscript.serializer.config.AtomRuleSerializer
import printscript.serializer.config.GrammarSerializer
import printscript.serializer.config.LeftRuleSerializer
import printscript.serializer.config.OptionalRuleSerializer
import printscript.serializer.config.OrRuleSerializer
import printscript.serializer.config.RepeatRuleSerializer
import printscript.serializer.config.SeqRuleSerializer

/**
 * Carga y deserializa archivos `grammar.config.json`.
 */
object JSONGrammarConfigReader : GrammarConfigReader {
    private val json =
        Json {
            ignoreUnknownKeys = true
            serializersModule = grammarModule()
        }

    private fun grammarModule() =
        SerializersModule {
            polymorphic(GrammarRule::class) {
                subclass(OrRule::class, OrRuleSerializer)
                subclass(AtomRule::class, AtomRuleSerializer)
                subclass(SeqRule::class, SeqRuleSerializer)
                subclass(LeftRule::class, LeftRuleSerializer)
                subclass(RepeatRule::class, RepeatRuleSerializer)
                subclass(OptionalRule::class, OptionalRuleSerializer)
            }
        }

    override fun read(path: Path): Grammar = read(path.readText())

    override fun read(input: InputStream): Grammar = read(input.bufferedReader().use { it.readText() })

    override fun read(jsonString: String): Grammar = json.decodeFromString(GrammarSerializer, jsonString)
}
