plugins {
    alias(libs.plugins.kotlin.jvm)
    id("printscript.publishing-conventions")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common"))
    implementation(project(":lexer"))
    implementation(project(":parser"))
    implementation(project(":type-checker"))
    implementation(project(":interpreter"))
    implementation(project(":linter"))
    implementation(project(":formatter"))

    testImplementation(project(":infrastructure"))

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
