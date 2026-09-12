package printscript.infrastructure.tck

import printscript.ErrorHandler
import printscript.InputChannel
import printscript.PrintChannel
import printscript.reader.CodeReader

object PrintScript {
    fun execute(
        version: String,
        codeReader: CodeReader,
        printChannel: PrintChannel,
        errorHandler: ErrorHandler,
        inputChannel: InputChannel,
    ) {
        val configs = PrintScriptConfigsLoader.load(version)
    }
}
