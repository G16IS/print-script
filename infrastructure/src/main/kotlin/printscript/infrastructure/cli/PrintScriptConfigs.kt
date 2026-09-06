package printscript.infrastructure.cli

import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.LinterConfig
import printscript.domain.TypeSystemConfig
import printscript.formatter.Formatter

class PrintScriptConfigs(
    val lang: LanguageConfig,
    val grammar: Grammar,
    val typeSystem: TypeSystemConfig,
    val linterConfig: LinterConfig,
    val formatter: Formatter,
)
