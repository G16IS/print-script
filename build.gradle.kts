plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    id("printscript.quality")
}

gradle.beforeProject {
    if (this != rootProject) {
        pluginManager.apply("printscript.quality")
    }
}
