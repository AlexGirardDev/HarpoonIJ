# Developing HarpoonIJ

Install the [Plugin DevKit](https://plugins.jetbrains.com/plugin/22851-plugin-devkit) plugin to work
on this project in IntelliJ IDEA.

For how the plugin is put together, the IdeaVim integration's hard rules, and the bar for tests that
touch it, see `AGENTS.md`. This file covers getting it to build, test and run.

## Building and testing

```bash
./gradlew build          # compiles the plugin and runs the test suite
./gradlew test           # tests only
./gradlew buildPlugin    # produces build/distributions/HarpoonIJ-<version>.zip, the marketplace artifact
./gradlew verifyPlugin   # runs the JetBrains Plugin Verifier against the recommended IDE releases
./gradlew runIde         # launches a sandbox IDE with the plugin and IdeaVim installed
```

The first invocation of any of these downloads the whole target IDE and the IdeaVim plugin, several
gigabytes unpacked into `~/.gradle`, so expect a long wait and plan disk for it. Everything after
that is cached: a warm `./gradlew test` is well under a minute, and `runIde` is limited by IDE
startup rather than by Gradle.

You do not need a matching JDK installed. `settings.gradle.kts` applies the foojay toolchain
resolver, so Gradle downloads the JDK named by `javaVersion` in `gradle.properties` if it is
missing. Gradle itself needs to run on JDK 17 or newer.

`build.gradle.kts` deliberately configures neither signing nor publishing. The IntelliJ Platform
Gradle Plugin already reads `PUBLISH_TOKEN`, `CERTIFICATE_CHAIN`, `PRIVATE_KEY` and
`PRIVATE_KEY_PASSWORD` from the environment by convention, and that convention applies *only* while
`signing.privateKeyFile` / `certificateChainFile` are unset. Declaring those file properties — as
this project used to, pointing at `/key` paths that existed nowhere — suppresses the environment
path, and because `signPlugin`'s `onlyIf` treats a non-existent file as unspecified, the task is
skipped even when every credential is present. `publishPlugin` then uploads the **unsigned** archive
without warning. Do not reintroduce those properties; see the comment in `build.gradle.kts`.

Neither task is part of `build`, so a checkout without credentials builds and tests fine —
`signPlugin` simply reports `SKIPPED`.

Do not launch the `.run/Run Plugin.run.xml` run configuration to try the plugin locally — despite
the name it runs `publishPlugin`. Use `./gradlew runIde`.

## Verifying a real IDE behaviour change

The headless suite has already been proven insufficient for anything involving the popup's key
handling: a green run coexisted with the popup being unusable in a real IDE for over a year (see
"The testing bar" in `AGENTS.md`). Treat `runIde` as mandatory for those changes, not optional.

```bash
./gradlew runIde
```

The sandbox IDE comes up with HarpoonIJ and IdeaVim both installed, and IdeaVim reads your own
`~/.ideavimrc`. Open any project with a handful of files, pin a few with
`Tools > Harpoon > Set Harpoon File N`, then open the popup with
`Tools > Harpoon > Show Harpoon Window` — or bind `ShowHarpoon` in `~/.ideavimrc` and drive it the
way a user would, which is the only way to exercise the IdeaVim path end to end. Check that the
popup opens in normal mode on the first entry, that `j`/`k` move without inserting text, that `dd`
edits the list, that `<cr>` opens the entry under the caret, and that Escape closes the popup.

The sandbox's config, system and log directories live under `.intellijPlatform/sandbox`; that log is
where `IdeaVimIntegration`'s warnings about not being attached to the popup show up.

## Versions

`gradle.properties` holds the plugin's own version, the target IntelliJ Platform version, the IdeaVim
version, the plugin's `sinceBuild`, and the JDK the platform requires — not `build.gradle.kts`.
Verify any version against the JetBrains plugin repository or the platform release data rather than
from memory; recalled ones are almost certainly stale.

`pluginVersion` is the one to bump when cutting a release. The Marketplace rejects a duplicate
version, and `0.2.0` is already published there, so a release must move past it.

Bump `platformVersion` and `ideaVimVersion` together, and update `javaVersion` and
`pluginSinceBuild` to match the new platform branch — see
[build number ranges](https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html).
`java-version` in `.github/workflows/build.yml` is a hand-copy of `javaVersion`; bump it with the
rest or CI compiles on the wrong JDK.

An `ideaVimVersion` bump is the risky half. `IdeaVimIntegration` and `IdeaVimIntegrationTest` are
what to check first; `AGENTS.md` lists the specific IdeaVim internals it depends on.

## Change notes

`CHANGELOG.md` is the source of truth. The `org.jetbrains.changelog` plugin renders the section
matching `pluginVersion` — or `[Unreleased]` when there is no such section — into `plugin.xml`'s
`<change-notes>` at build time. **Do not hand-write `<change-notes>` in `plugin.xml`; it is
overwritten silently.** Write entries under `[Unreleased]` as you work; `./gradlew patchChangelog`
promotes them to a version section when a release is cut.

The Gradle plugin needs no `changelog { }` block — the IntelliJ Platform Gradle Plugin wires the two
together as soon as the changelog plugin is applied.

## CI

`.github/workflows/build.yml` runs `build`, `buildPlugin` and `verifyPlugin` on every push and pull
request. It uploads three artifacts: `plugin-distribution` (the installable zip),
`test-results`, and `pluginVerifier-result`. Grab `plugin-distribution` for the real-IDE check below
rather than rebuilding locally.

`.github/workflows/ideavim-canary.yml` runs the test suite weekly against whatever IdeaVim is newest
on the Marketplace, via `./gradlew test -PideaVimVersion=<latest>`. It is early warning for the
failure mode that has broken this plugin most often. It only helps to the extent the popup tests
assert observable behaviour — read "The testing bar" in `AGENTS.md` before touching them. If it goes
red, reproduce in a real IDE before changing any test.

There is no release or publishing workflow; releases are still manual.

## Tests

Tests live in `src/test/java` and run on the IntelliJ Platform's light fixtures
(`BasePlatformTestCase`). IdeaVim is a real test dependency, so the popup's Vim behaviour is
exercised against the real thing. What those tests are allowed to assert is not a free choice — read
"The testing bar" in `AGENTS.md` before writing one.

Three things the fixtures make awkward, and how the tests handle them:

- `HarpoonState` caches lists in static state keyed by project name, and the light fixture reuses
  one project for the whole run. `HarpoonTestCase` clears both that cache and the persisted
  `HarpoonJumpList` property around every test, and works with real files in a scratch directory
  because Harpoon resolves entries through `LocalFileSystem`.
- `EditorTextField` only creates its editor once it is in a component hierarchy. Calling
  `setDisposedWith(...)` and then `getEditor(true)` gets a real editor headlessly.
- The Vim mode is application-global, so a test that leaves it in insert mode changes what the next
  test's first keystroke means. The IdeaVim-facing classes reset it in `setUp` and `tearDown`.

`ShowHarpoon` only forwards to a popup that is actually on screen, so the tests drive
`HarpoonDialog` directly rather than going through the `NextHarpoonItem` / `PreviousHarpoonItem` /
`SelectHarpoonItem` actions.
