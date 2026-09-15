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
import printscript.io.ReadInputHandler
import printscript.reader.CodeReader
import printscript.reader.JSONFormatterRulesConfigReader
import printscript.usecases.ExecuteCode
import printscript.usecases.FormatCode
import printscript.util.Result

object PrintScript {
    fun execute(
        version: String,
        codeReader: CodeReader,
        printChannel: PrintChannel,
        errorHandler: ErrorHandler,
        inputChannel: InputChannel,
    ) {
        val configs: PrintScriptConfigs = PrintScriptConfigsLoader.load(version)

        val sideEffectManager = DefaultSideEffectManager(listAllSideEffectHandlers(printChannel, inputChannel))

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

    private fun listAllSideEffectHandlers(
        printChannel: PrintChannel,
        inputChannel: InputChannel,
    ): List<SideEffectHandler> =
        listOf(
            PrintHandler(printChannel),
            ReadInputHandler(inputChannel),
        )
}
