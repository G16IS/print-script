plugins {
    alias(libs.plugins.kotlin.jvm)
    application

    // Continuous Deployment
    id("printscript.publishing-conventions")
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("printscript.cli.MainKt")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":application"))
    implementation(project(":infrastructure"))
    implementation(project(":formatter"))
    implementation(project(":type-checker"))
    implementation(libs.clikt)

    testImplementation("org.junit.jupiter:junit-jupiter:6.0.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
