plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
    implementation("org.jlleitschuh.gradle.ktlint:org.jlleitschuh.gradle.ktlint.gradle.plugin:14.2.0")
    implementation("dev.detekt:dev.detekt.gradle.plugin:2.0.0-alpha.6")
}

kotlin {
    jvmToolchain(21)
}
