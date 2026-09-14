plugins {
    alias(libs.plugins.kotlin.jvm)

    // Continuous Deployment
    id("printscript.publishing-conventions")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
