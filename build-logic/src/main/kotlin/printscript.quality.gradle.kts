import dev.detekt.gradle.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import printscript.InstallGitHooks

if (project != rootProject) {
    pluginManager.apply("org.jetbrains.kotlin.jvm")
    pluginManager.apply("org.jlleitschuh.gradle.ktlint")
    pluginManager.apply("dev.detekt")


    extensions.configure<KtlintExtension> {
        android.set(false)
        outputToConsole.set(true)
        verbose.set(true)
        coloredOutput.set(true)
        relative.set(true)
    }

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig.set(true)
        allRules.set(false)
        parallel.set(true)
        config.setFrom(files("${project.rootDir}/config/detekt/detekt.yml"))
    }

    tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
        jvmTarget.set("21")
        reports {
            html.required.set(false)
            checkstyle.required.set(false)
            sarif.required.set(false)
            markdown.required.set(false)
        }
    }
}

if (project == rootProject) {
    tasks.register<InstallGitHooks>("installGitHooks") {
        group = "build setup"
        description = "Installs the repository git hooks if they are not already installed."
        repoDirectory.convention(layout.projectDirectory)
        hooksDirectoryName.convention("hooks")
    }
}
