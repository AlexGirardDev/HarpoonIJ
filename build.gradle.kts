plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.6.10"
    id("org.jetbrains.intellij") version "1.10.1"
}

group = "ca.alexgirard"
version = "0.1.3"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2023.1")
    type.set("IC") // Target IDE Platform
    updateSinceUntilBuild.set(false)
    plugins.set(listOf("IdeaVIM:2.0.0"))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }



    patchPluginXml {
        sinceBuild.set("223")
    }
    buildSearchableOptions {
        enabled = false
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
