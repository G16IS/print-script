package printscript.infrastructure.reader

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.readText
import printscript.domain.FormatterRulesConfig
import printscript.infrastructure.serializer.config.FormatterRulesConfigSerializer
import printscript.reader.FormatterRulesConfigReader

/**
 * Carga el YAML de preferencias de usuario (misma forma semántica que el JSON de lenguaje).
 */
object YAMLFormatterRulesConfigReader : FormatterRulesConfigReader {
    private val yaml =
        Yaml(
            configuration =
                YamlConfiguration(
                    strictMode = false,
                ),
        )

    override fun read(path: Path): FormatterRulesConfig = read(path.readText())

    override fun read(input: InputStream): FormatterRulesConfig = read(input.bufferedReader().use { it.readText() })

    override fun read(text: String): FormatterRulesConfig = yaml.decodeFromString(FormatterRulesConfigSerializer, text)
}
