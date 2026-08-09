plugins {
    alias(libs.plugins.kotlin.jvm)
}


repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common"))
    implementation(project(":lexer"))
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.8")
}
