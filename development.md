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
`java-version` in each of the three workflows under `.github/workflows/` is a hand-copy of
`javaVersion`; bump all of them with the rest or CI compiles on the wrong JDK.

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

`.github/workflows/release.yml` publishes to the JetBrains Marketplace. It runs **only** on a
published GitHub Release — never on a merge — so nothing that lands on `master` can reach users on
its own. See "Releasing" below.

## Releasing

Publishing is automated, the decision to publish is not. `release.yml` fires on a **published
GitHub Release** and nothing else; creating that release is the approval gate. Until the four
secrets below exist the workflow is inert — it refuses to publish and fails on the
`Require signing material` step.

### One-time setup: the four repository secrets

Add all four under **Settings → Secrets and variables → Actions → Repository secrets**. The names
must match exactly: they are the environment variables the IntelliJ Platform Gradle Plugin reads by
convention, which is why `build.gradle.kts` configures no `signing { }` or `publishing { }` block.

| Secret | What it is | How to create it |
|---|---|---|
| `PUBLISH_TOKEN` | Marketplace permanent token; authorises uploading plugin updates. | JetBrains Marketplace profile → **My Tokens** → name it → **Generate Token**. Shown once — copy it immediately. |
| `PRIVATE_KEY` | The signing private key, as PEM **text** (not a path). | `openssl genpkey -aes-256-cbc -algorithm RSA -out private_encrypted.pem -pkeyopt rsa_keygen_bits:4096`, then `openssl rsa -in private_encrypted.pem -out private.pem`. Paste the contents of `private.pem`. |
| `PRIVATE_KEY_PASSWORD` | The passphrase chosen during `genpkey`. | You choose it at key generation. |
| `CERTIFICATE_CHAIN` | The self-signed certificate, as PEM **text**. | `openssl req -key private.pem -new -x509 -days 3650 -out chain.crt`. Paste the contents of `chain.crt`. |

Use **`-days 3650`**, not the `365` in the JetBrains
[plugin signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html) docs. This plugin
releases roughly once a year, so a one-year certificate expires almost exactly between releases and
breaks a release for a reason nobody will remember.

Paste each PEM **including** its `-----BEGIN…` / `-----END…` lines and the trailing newline; GitHub
secrets preserve multi-line values. Keep an offline backup of `private.pem` and the passphrase —
losing them means generating a new key. `PUBLISH_TOKEN` is the dangerous one if it leaks: it is
account-level, so it can push an update to any plugin the account owns, and that update reaches
existing users through the IDE's normal update path. Revoke and regenerate it from **My Tokens**.

### The release ritual

1. Write bullets under `## [Unreleased]` in `CHANGELOG.md` as you work.
2. Bump `pluginVersion` in `gradle.properties`, run `./gradlew patchChangelog` to promote
   `[Unreleased]` into a version section, commit, push. `patchChangelog` is a **local** step; CI
   does not run it.
3. Wait for `build.yml` to go green, then **download the `plugin-distribution` artifact from that
   run and install it in a real IDE** (*Settings → Plugins → ⚙ → Install Plugin from Disk…*).
   Open a project, pin a few files, open the popup, and walk the popup checks listed under
   "Verifying a real IDE behaviour change" above.

   **This step is required, not a recommendation, and it is required for every release** — not only
   for releases that touched popup code. This project once shipped with sixteen green tests
   covering the popup's Vim normal-mode behaviour while that behaviour was completely broken in a
   real IDE: the tests asserted an application-global state machine, so they were true and
   meaningless. The feature stayed broken for over a year. No automated gate in this repository
   catches that class of defect; installing the artifact and pressing the keys does.
4. Create the GitHub Release. `gh` tags the pushed commit for you, so make sure step 2 is on
   `master` first. Put the new `CHANGELOG.md` section in a file and pass it as the notes:

   ```bash
   gh release create v0.3.0 --title v0.3.0 --notes-file release-notes.md
   ```

   Tag as `vMAJOR.MINOR.PATCH`. Existing tags are inconsistent (`v0.1.7`, `0.2`, one named
   `release`); `v0.3.0` is the convention going forward. The workflow accepts `0.3.0` too, but
   rejects anything that is not exactly `pluginVersion` — so `0.3` or `release` would fail.
5. `release.yml` asserts all four secrets are present, checks out the tag, asserts the tag matches
   `pluginVersion`, runs `test` and `verifyPlugin` on that exact commit, then signs and publishes
   in a single Gradle invocation, and attaches the signed zip to the GitHub Release.
6. A successful publish is **not** immediate availability. JetBrains reviews every update against
   their approval criteria before it goes live, normally within two business days. That review is
   about policy compliance, not about whether the popup works — it is not a substitute for step 3.

Marking the GitHub Release as a **pre-release** deliberately does nothing: the job is skipped. This
plugin publishes to the default (Stable) channel only, so a pre-release has no separate channel to
go to and must not be pushed to every existing user. A pre-release is a GitHub-only artifact. Do
not add a Marketplace release channel to work around this — a non-default channel requires users to
add a custom plugin repository URL, so its audience is effectively nobody. This project carried a
`beta` channel from 2024-05 to 2024-11, shipped nothing through it, and deleted it the day 0.2.0
went out to Stable.

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
