plugins {
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}
dependencies {
    implementation(project(":common"))
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}
