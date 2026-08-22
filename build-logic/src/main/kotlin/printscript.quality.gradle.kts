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
}

detekt {
    buildUponDefaultConfig.set(true)
    parallel.set(true)
}

tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
    jvmTarget.set("21")
    reports {
        html.required.set(true)
        checkstyle.required.set(true)
        sarif.required.set(true)
    }
}
