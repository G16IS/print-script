package printscript

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Optional
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Starts a JVM to run the PrintScript CLI")
abstract class PrintScriptExec : JavaExec() {
    @get:Input
    abstract val command: Property<String>

    /** PrintScript language version (`1.0`, `1.1`). Comes from `-PpsVersion`. */
    @get:Input
    @get:Optional
    abstract val languageVersion: Property<String>

    override fun exec() {
        val cliCommand = command.get()
        val current = args.toMutableList()
        if (current.isEmpty() || current.first() != cliCommand) {
            current.add(0, cliCommand)
        }
        // The CLI flag belongs to the root command, before the subcommand.
        languageVersion.orNull?.let { version ->
            current.add(0, version)
            current.add(0, "--version")
        }
        args = current
        super.exec()
    }
}
