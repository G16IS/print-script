package printscript.edition

import printscript.expression.ExpressionEvaluator
import printscript.expression.binaryoperation.TypeConfiguration
import printscript.parse.RuleHandler
import printscript.statement.StatementExecutor
import printscript.typechecker.ExpressionKindHandlerFactory

// LanguageKit is a data class that contains all the components
// needed for a specific version of the PrintScript language.
data class LanguageKit(
    val version: LanguageVersion,
    val resourceSuffix: String,
    val parserHandlers: List<RuleHandler>,
    val kindHandlerFactory: ExpressionKindHandlerFactory,
    val typeConfiguration: TypeConfiguration,
    val evaluators: List<ExpressionEvaluator>,
    val executors: List<StatementExecutor>,
)
