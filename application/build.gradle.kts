plugins {
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":infrastructure"))
    implementation(project(":lexer"))

    implementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}