import org.gradle.api.tasks.SourceSetContainer
import printscript.PrintScriptExec

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    id("printscript.quality")
}

gradle.beforeProject {
    if (this != rootProject) {
        pluginManager.apply("printscript.quality")
    }
}

listOf(
    Triple("ps-run", "run", "Execute a PrintScript file"),
    Triple("ps-lint", "lint", "Lint a PrintScript file"),
    Triple("ps-check", "check", "Check that a PrintScript file is formatted"),
    Triple("ps-format", "format", "Format a PrintScript file and print it to stdout"),
    Triple("ps-typecheck", "typecheck", "Type-check a PrintScript file"),
).forEach { (taskName, commandName, taskDescription) ->
    tasks.register<PrintScriptExec>(taskName) {
        group = "printscript"
        description = taskDescription
        command.set(commandName)
        dependsOn(":infrastructure:classes")
        mainClass.set("printscript.infrastructure.cli.MainKt")
        classpath(
            provider {
                project(":infrastructure")
                    .extensions
                    .getByType(SourceSetContainer::class.java)
                    .getByName("main")
                    .runtimeClasspath
            },
        )
        standardInput = System.`in`
        args(commandName)
        argumentProviders.add {
            listOfNotNull(
                findProperty("file")?.toString()
                    ?: System.getProperty("printscript.file"),
            )
        }
        notCompatibleWithConfigurationCache("PrintScript CLI takes a source file from the command line")
    }
}
