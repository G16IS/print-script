plugins {
    alias(libs.plugins.kotlin.jvm)

    // Continuous Deployment
    id("printscript.publishing-conventions")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common"))

    testImplementation(project(":lexer"))
    testImplementation(project(":parser"))
    testImplementation(project(":infrastructure"))
    testImplementation(project(":application"))
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStackTraces = true
        showCauses = true
        showExceptions = true
    }
}

kotlin {
    jvmToolchain(21)
}
