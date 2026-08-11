plugins {
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common"))
}

kotlin {
    jvmToolchain(21)
}
