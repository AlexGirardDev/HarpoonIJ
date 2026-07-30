# Project agent memory

This file is the project's committed home for project-intrinsic agent knowledge: build, test, release, architecture, and sharp-edge notes that should travel with the code.

## Build and test

See `development.md` for the commands, the toolchain setup, and the test-fixture gotchas. In short:
`./gradlew build` compiles and tests; the first run downloads ~1.5 GB of IDE.

Versions (platform, IdeaVim, `sinceBuild`, JDK) live in `gradle.properties`, not in
`build.gradle.kts`. Verify any version against the JetBrains plugin repository or the platform
release data rather than from memory — the ones you recall are almost certainly stale.

## Sharp edges

- **IdeaVim is the fragile part.** It is an optional dependency, its internals move between
  releases, and this integration has broken repeatedly (see `git log` for the recurring "forcing
  normal mode" fixes). All of it is confined to `IdeaVimIntegration`; keep it that way, and never
  touch that class' methods without checking `IdeaVimIntegration.isAvailable()` first — the IdeaVim
  classes are simply absent when the plugin is not installed.
- **`HarpoonState` is static state keyed by `project.getName()`**, not by project instance. Two
  open projects with the same name share one Harpoon list.
- **`src/main` is Java on purpose.** `todo.md` wants a Kotlin migration eventually; do not mix that
  into an unrelated change.
- **`.run/Run Plugin.run.xml` runs `publishPlugin`**, despite the name. Do not launch it to try the
  plugin locally; use `./gradlew runIde`.

## Known behaviour gaps

Confirmed by test probes, deliberately left unchanged pending a maintainer decision:

- Slot positions do not survive a restart. `HarpoonState.SetItem` persists a null-filtered list, so
  a list of `[a, -, -, -, e]` comes back as `[a, e]` and "Goto Harpoon File 5" changes target.
- `HarpoonState.SetFiles` with an empty list never writes, so clearing every entry in the popup is
  undone by a restart.
- `ShowHarpoon.NavigateToIndex` does not check `VirtualFile.isValid()` the way
  `GoToHarpoonActionBase` does; selecting a popup entry whose file was deleted mid-session throws.
- `HarpoonState.GetItem` throws `IndexOutOfBoundsException` for a negative index rather than
  returning null.

## Maintaining this file

Keep this file for knowledge useful to almost every future agent session in this project.
Do not repeat what the codebase already shows; point to the authoritative file or command instead.
Prefer rewriting or pruning existing entries over appending new ones.
When updating this file, preserve this bar for all agents and keep entries concise.
