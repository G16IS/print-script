package printscript

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Starts a JVM to run the PrintScript CLI")
abstract class PrintScriptExec : JavaExec() {
    @get:Input
    abstract val command: Property<String>

    override fun exec() {
        val cliCommand = command.get()
        val current = args
        if (current.isEmpty() || current.first() != cliCommand) {
            args = listOf(cliCommand) + current
        }
        super.exec()
    }
}
