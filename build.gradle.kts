import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.18.1"
}

group = "ca.alexgirard"
version = "0.2.0"

val platformVersion = providers.gradleProperty("platformVersion").get()
val ideaVimVersion = providers.gradleProperty("ideaVimVersion").get()
val javaVersion = providers.gradleProperty("javaVersion").get().toInt()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    // Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
    intellijPlatform {
        intellijIdea(platformVersion)
        plugin("IdeaVIM", ideaVimVersion)

        // JUnit 4 based platform fixtures (BasePlatformTestCase and friends).
        testFramework(TestFrameworkType.Platform)
    }

    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    buildSearchableOptions = false

    pluginConfiguration {
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            // Deliberately open-ended, matching the pre-2.x
            // `updateSinceUntilBuild = false` behaviour.
            untilBuild = provider { null }
        }
    }

    // Marketplace signing/publishing credentials only exist on the release
    // machine. Referencing them here is inert: `signPlugin` and `publishPlugin`
    // are never part of `build`, so a checkout without the key files or env
    // vars still builds and tests cleanly.
    signing {
        certificateChainFile = file("/key/chain.crt")
        privateKeyFile = file("/key/certificate/private.pem")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }

    pluginVerification {
        ides {
            // Verify against exactly the platform we compile against; it is
            // already downloaded, so `verifyPlugin` costs nothing extra.
            current()
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
}

tasks.test {
    useJUnit()
}
