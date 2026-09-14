package printscript.tck

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
import printscript.usecases.ExecuteCode
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

    private fun listAllSideEffectHandlers(
        printChannel: PrintChannel,
        inputChannel: InputChannel,
    ): List<SideEffectHandler> =
        listOf(
            PrintHandler(printChannel),
            ReadInputHandler(inputChannel),
        )
}
