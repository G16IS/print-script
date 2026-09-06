plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("printscript.infrastructure.cli.MainKt")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":cli"))
    implementation(project(":application"))
    implementation(project(":formatter"))
    implementation(project(":type-checker"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kaml)

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
