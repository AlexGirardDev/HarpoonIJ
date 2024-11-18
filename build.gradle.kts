plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.0"
    id("org.jetbrains.intellij") version "1.15.0"
}

group = "ca.alexgirard"
version = "0.2.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}


// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2024.1.1")
    type.set("IC") // Target IDE Platform
    updateSinceUntilBuild.set(false)
    plugins.set(listOf("IdeaVIM:2.16.0"))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("231")
    }
    buildSearchableOptions {
        enabled = false
    }

    
    signPlugin {
        certificateChainFile.set(file("/key/chain.crt"))
        privateKeyFile.set(file("/key/certificate/private.pem"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }
    

    publishPlugin {
        channels.set(listOf("beta"))
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
