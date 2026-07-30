plugins {
    // Lets Gradle provision the JDK the IntelliJ Platform toolchain needs on
    // machines that do not already have that exact version installed.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "HarpoonIJ"
