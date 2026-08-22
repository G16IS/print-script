plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jlleitschuh.gradle.ktlint")
    id("dev.detekt")
}

kotlin {
    jvmToolchain(21)
}

ktlint {
    android.set(false)
    outputToConsole.set(true)
    verbose.set(true)
    coloredOutput.set(true)
    relative.set(true)
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    parallel = true
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
