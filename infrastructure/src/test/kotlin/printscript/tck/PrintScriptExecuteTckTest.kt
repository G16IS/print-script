package printscript.tck

import java.util.Optional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.definitions.ErrorHandler
import printscript.definitions.InputChannel
import printscript.definitions.PrintChannel
import printscript.reader.CharPosition
import printscript.reader.CodeReader

class PrintScriptExecuteTckTest {
    @Test
    fun `reports out of memory error to error handler`() {
        val errorHandler = TestErrorHandler()
        val oomReader =
            object : CodeReader {
                override fun read(): Optional<Char> = throw OutOfMemoryError("Java heap space")

                override fun peek(): Optional<Char> = throw OutOfMemoryError("Java heap space")

                override fun currentPosition(): CharPosition = CharPosition(1, 1)
            }

        PrintScript.execute(
            version = "1.0",
            codeReader = oomReader,
            printChannel =
                object : PrintChannel {
                    override fun print(text: String) = Unit
                },
            errorHandler = errorHandler,
            inputChannel =
                object : InputChannel {
                    override fun input(askMessage: String): String = ""
                },
        )

        assertTrue(errorHandler.errors.isNotEmpty())
        assertEquals("Java heap space", errorHandler.errors.first())
    }

    private class TestErrorHandler : ErrorHandler {
        val errors = mutableListOf<String>()

        override fun handleErrorMessage(errorMessage: String) {
            errors += errorMessage
        }
    }
}
