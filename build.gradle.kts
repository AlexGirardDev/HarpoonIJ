import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.18.1"
    // Renders CHANGELOG.md into the plugin's change-notes. The IntelliJ Platform
    // Gradle Plugin wires this up on its own as soon as the plugin is applied,
    // so there is deliberately no `changelog { }` block here.
    id("org.jetbrains.changelog") version "2.5.0"
}

group = "ca.alexgirard"
version = providers.gradleProperty("pluginVersion").get()

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

    // There is deliberately no `signing { }` or `publishing { }` block.
    //
    // Setting `signing.privateKeyFile` / `certificateChainFile` does not merely
    // fail to work without those files -- it suppresses the environment-variable
    // path entirely. The plugin's conventions read PRIVATE_KEY / CERTIFICATE_CHAIN
    // only when the corresponding *File property is unset, and signPlugin's
    // `onlyIf` treats a file that does not exist as unspecified. With both
    // declared, signPlugin is skipped even when every credential is present, and
    // publishPlugin then falls back to the *unsigned* archive without warning.
    //
    // Leaving these unset is what makes the documented CI path work: export
    // PUBLISH_TOKEN, CERTIFICATE_CHAIN, PRIVATE_KEY and PRIVATE_KEY_PASSWORD and
    // the conventions pick them up. A checkout without them still builds and
    // tests cleanly; signPlugin simply reports SKIPPED.

    // There is also deliberately no `pluginVerification { ides { ... } }` block.
    // When none is configured the plugin applies `recommended()`, which tracks
    // the IDE releases matching this plugin's since/until range. Pinning
    // `current()` here was *narrower* than that default, and would never pick up
    // a new platform branch.
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
}

tasks.test {
    useJUnit()
}
