package usecases

import printscript.domain.Grammar
import printscript.domain.LanguageConfig
import printscript.domain.TypeSystemConfig
import printscript.syntax.SyntaxProgram
import printscript.typechecker.DefaultTypeCheckerFactory
import printscript.typechecker.TypeError

object InterpretCode {
    fun interpretCode(
        langConfig: LanguageConfig,
        grammar: Grammar,
        typeSystem: TypeSystemConfig,
        path: String,
    ): SyntaxProgram {
        val program = ParseProgram.parse(langConfig, grammar, path)

        val report = DefaultTypeCheckerFactory.create(typeSystem).check(program)

        if (!report.isOk) {
            failTypeCheck(report.errors)
        }

        return program
    }

    private fun failTypeCheck(errors: List<TypeError>): Nothing {
        val messages =
            errors.joinToString("\n") { typeError ->
                val position = typeError.location.start
                "  - ${typeError.message} @ ${position.line}:${position.col}"
            }
        error("El chequeo de tipos falló:\n$messages")
    }
}
