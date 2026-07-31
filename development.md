# Developing HarpoonIJ

Install the [Plugin DevKit](https://plugins.jetbrains.com/plugin/22851-plugin-devkit) plugin to work
on this project in IntelliJ IDEA.

## Building and testing

```bash
./gradlew build          # compiles the plugin and runs the test suite
./gradlew test           # tests only
./gradlew buildPlugin    # produces build/distributions/HarpoonIJ-<version>.zip
./gradlew verifyPlugin   # runs the JetBrains Plugin Verifier against the target IDE
./gradlew runIde         # launches a sandbox IDE with the plugin installed
```

The first build downloads the whole target IDE (roughly 1.5 GB) plus the IdeaVim plugin, so expect
it to take a while. Everything after that is cached in `~/.gradle`.

You do not need a matching JDK installed. `settings.gradle.kts` applies the foojay toolchain
resolver, so Gradle downloads the JDK named by `javaVersion` in `gradle.properties` if it is
missing. Gradle itself needs to run on JDK 17 or newer.

`signPlugin` and `publishPlugin` point at key files under `/key` and at the `PRIVATE_KEY_PASSWORD`
and `PUBLISH_TOKEN` environment variables. Those only exist on the release machine. Neither task is
part of `build`, so a checkout without them builds and tests fine.

## Versions

`gradle.properties` holds the target IntelliJ Platform version, the IdeaVim version, the plugin's
`sinceBuild`, and the JDK the platform requires. Bump `platformVersion` and `ideaVimVersion`
together, and update `javaVersion` and `pluginSinceBuild` to match the new platform branch — see
[build number ranges](https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html).

## The IdeaVim integration

IdeaVim is an optional dependency whose internals move between releases, and it is the part of this
plugin that has broken most often. Every call into it goes through `IdeaVimIntegration`; nothing
else in `src/main` touches a `com.maddyhome.idea.vim` class. When you bump `ideaVimVersion`, that
class and `IdeaVimIntegrationTest` are what to check first.

Its classes are only on the classpath when the IdeaVim plugin is installed, so every method of
`IdeaVimIntegration` that reaches into IdeaVim checks `IdeaVimIntegration.isAvailable()` itself and
does nothing when IdeaVim is absent. Any new method there has to do the same.

## Tests

Tests live in `src/test/java` and run on the IntelliJ Platform's light fixtures
(`BasePlatformTestCase`). IdeaVim is a real test dependency, so the tests covering the popup's
normal-mode forcing and the `<cr>` remap assert what IdeaVim itself reports rather than which
internal calls were made.

Two things the fixtures make awkward, and how the tests handle them:

- `HarpoonState` caches lists in static state keyed by project name, and the light fixture reuses
  one project for the whole run. `HarpoonTestCase` clears both that cache and the persisted
  `HarpoonJumpList` property around every test.
- `EditorTextField` only creates its editor once it is in a component hierarchy. Calling
  `setDisposedWith(...)` and then `getEditor(true)` gets a real editor headlessly.

`ShowHarpoon` only forwards to a popup that is actually on screen, so the tests drive
`HarpoonDialog` directly rather than going through the `NextHarpoonItem` / `PreviousHarpoonItem` /
`SelectHarpoonItem` actions.
