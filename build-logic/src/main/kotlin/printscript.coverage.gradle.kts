plugins {
    id("org.jetbrains.kotlinx.kover")
}

repositories {
    mavenCentral()
}

kover {
    reports {
        verify {
            rule {
                minBound(80)
            }
        }
    }
}

