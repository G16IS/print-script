import org.gradle.api.tasks.SourceSetContainer
import printscript.PrintScriptExec
import printscript.CoverageReportTask

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    id("printscript.quality")
    id("printscript.coverage")
}

gradle.beforeProject {
    if (this != rootProject) {
        pluginManager.apply("printscript.quality")
        pluginManager.apply("printscript.coverage")
    }
}

val coverageModules = listOf(
    "common", "lexer", "infrastructure", "parser", "type-checker",
    "application", "interpreter", "linter", "formatter", "cli",
)

dependencies {
    kover(project(":common"))
    kover(project(":lexer"))
    kover(project(":infrastructure"))
    kover(project(":parser"))
    kover(project(":type-checker"))
    kover(project(":application"))
    kover(project(":interpreter"))
    kover(project(":linter"))
    kover(project(":formatter"))
    kover(project(":cli"))
}

allprojects {
    group = "com.g16is.printscript"
    version = project.findProperty("version") as? String ?: "0.0.0-SNAPSHOT"
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
        dependsOn(":cli:classes")
        mainClass.set("printscript.cli.MainKt")
        classpath(
            provider {
                project(":cli")
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

kover {
    reports {
        verify {
            rule { minBound(80) }
        }
    }
}

tasks.register<CoverageReportTask>("coverageReport") {
    group = "verification"
    description = "Imprime un resumen de cobertura por módulo"
    dependsOn(coverageModules.map { ":$it:koverXmlReport" })
    moduleReports.set(
        coverageModules.associateWith { name ->
            "${project(":$name").layout.buildDirectory.get()}/reports/kover/report.xml"
        },
    )
}

tasks.register("verifyAllCoverage") {
    group = "verification"
    description = "Corre koverVerify (80% mínimo) en todos los módulos"
    dependsOn(coverageModules.map { ":$it:koverVerify" })
}
