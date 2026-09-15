package printscript.tck

import java.io.InputStream
import java.io.Writer
import printscript.ErrorHandler
import printscript.InputChannel
import printscript.PrintChannel
import printscript.SideEffectHandler
import printscript.config.PrintScriptConfigs
import printscript.edition.LanguageCatalog
import printscript.io.DefaultSideEffectManager
import printscript.io.PrintHandler
import printscript.io.ReadEnvHandler
import printscript.io.ReadInputHandler
import printscript.reader.CodeReader
import printscript.reader.JSONFormatterRulesConfigReader
import printscript.reader.JSONLinterConfigReader
import printscript.usecases.ExecuteCode
import printscript.usecases.FormatCode
import printscript.usecases.LintProgram
import printscript.util.Result

object PrintScript {
    fun execute(
        version: String,
        codeReader: CodeReader,
        printChannel: PrintChannel,
        errorHandler: ErrorHandler,
        inputChannel: InputChannel,
    ) {
        try {
            val configs: PrintScriptConfigs = PrintScriptConfigsLoader.load(version)

            val sideEffectManager =
                DefaultSideEffectManager(listAllSideEffectHandlersByVersion(version, printChannel, inputChannel))

            val languageKitResult =
                when (val kit = LanguageCatalog.of(version, sideEffectManager)) {
                    is Result.Err -> return errorHandler.handleErrorMessage("version $version not found")
                    is Result.Ok -> kit
                }

            ExecuteCode.executeForTck(
                configs,
                codeReader,
                errorHandler,
                languageKitResult.value,
            )
        } catch (e: OutOfMemoryError) {
            errorHandler.handleErrorMessage(e.message ?: "Out of memory")
        }
    }

    fun format(
        version: String,
        codeReader: CodeReader,
        config: InputStream,
        writer: Writer,
    ) {
        val userRules = JSONFormatterRulesConfigReader.read(config)
        val configs = PrintScriptConfigsLoader.load(version, userRules)

        val languageKitResult =
            when (val kit = LanguageCatalog.of(version, DefaultSideEffectManager())) {
                is Result.Err -> return
                is Result.Ok -> kit
            }

        FormatCode.formatForTck(
            configs,
            codeReader,
            writer,
            languageKitResult.value,
        )
    }

    fun lint(
        version: String,
        codeReader: CodeReader,
        config: InputStream,
        errorHandler: ErrorHandler,
    ) {
        try {
            // El catálogo va antes del loader: para una versión desconocida el loader tira
            // `Missing resource` y perderíamos el mensaje.
            val languageKit =
                when (val kit = LanguageCatalog.of(version, DefaultSideEffectManager())) {
                    is Result.Err -> return errorHandler.handleErrorMessage("version $version not found")
                    is Result.Ok -> kit.value
                }

            val configs = PrintScriptConfigsLoader.load(version, JSONLinterConfigReader.read(config))

            LintProgram.lintForTck(configs, codeReader, errorHandler, languageKit)
        } catch (e: OutOfMemoryError) {
            errorHandler.handleErrorMessage(e.message ?: "Out of memory")
        }
    }

    private fun listAllSideEffectHandlers(
        printChannel: PrintChannel,
        inputChannel: InputChannel,
    ): List<SideEffectHandler> =
        listOf(
            PrintHandler(printChannel),
            ReadInputHandler(inputChannel, printChannel),
        )

    private fun listAllSideEffectHandlersByVersion(
        version: String,
        printChannel: PrintChannel,
        inputChannel: InputChannel,
    ): List<SideEffectHandler> =
        when (version) {
            "1.0" -> listAllSideEffectHandlers(printChannel, inputChannel)
            "1.1" ->
                listAllSideEffectHandlers(printChannel, inputChannel) +
                    ReadEnvHandler()
            else -> listOf()
        }
}
