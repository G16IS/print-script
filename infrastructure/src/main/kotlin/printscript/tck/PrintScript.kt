package printscript.tck

import printscript.ErrorHandler
import printscript.InputChannel
import printscript.PrintChannel
import printscript.reader.CodeReader

object PrintScript {
    @Suppress("UnusedParameter")
    fun execute(
        version: String,
        codeReader: CodeReader,
        printChannel: PrintChannel,
        errorHandler: ErrorHandler,
        inputChannel: InputChannel,
    ) {
        PrintScriptConfigsLoader.load(version)
    }
}
