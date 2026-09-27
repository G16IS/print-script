plugins {
    alias(libs.plugins.kotlin.jvm)

    // Continuous Deployment
    id("com.g16is.conventions.publishing")
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
