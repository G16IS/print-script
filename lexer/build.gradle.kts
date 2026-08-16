plugins {
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}
dependencies {
    implementation(project(":common"))
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.8")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}
