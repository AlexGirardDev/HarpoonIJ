# Project agent memory

HarpoonIJ is an IntelliJ Platform plugin — a port of ThePrimeagen's NeoVim
[Harpoon](https://github.com/ThePrimeagen/harpoon). It pins files to numbered slots, jumps to them
by hotkey, and shows the list in a popup that behaves like a Vim buffer.

This file is the project's committed home for project-intrinsic agent knowledge: architecture,
sharp edges, the rules that past defects bought at high cost. It owns the IdeaVim story and the
testing bar. `development.md` owns build, toolchain, versions and CI; `README.md` owns the
user-facing feature and keybinding documentation. Do not duplicate across the three.

## How it fits together

One flat package, `src/main/java/ca/alexgirard/harpoonIJ`. `src/main` is Java on purpose —
`todo.md` wants a Kotlin migration eventually; do not mix that into an unrelated change.

- **State** — `HarpoonState`. A static `Map<projectName, List<VirtualFile>>` plus a
  `PropertiesComponent` list under the key `HarpoonJumpList` holding absolute paths. A `null`
  element is an empty slot. Every other class reads and writes the list through it.
- **Settings** — `AppSettingsState` (application service: popup width/height/font size, and
  `enterRemap`), surfaced by `AppSettingsConfigurable` + `HarpoonIjSettingsComponent`.
- **Pin actions** — `SetHarpoonFileAction1..5` over `SetHarpoonFileActionBase` write the current
  file to a fixed slot; `AddToHarpoonAction` writes it to the first empty slot, or appends.
- **Jump actions** — `GotoHarpoon1..5Action` over `GoToHarpoonActionBase` open the file in a fixed
  slot. There are five hotkey slots, but the list itself is not capped at five.
- **Popup** — `ShowHarpoon` (the action) renders the list and shows `HarpoonDialog` (the
  `DialogWrapper`). `NextHarpoonItem` / `PreviousHarpoonItem` / `SelectHarpoonItem` are separate
  actions that forward through static hooks on `ShowHarpoon` to whichever dialog is currently
  showing, and do nothing when none is.
- **IdeaVim** — `IdeaVimIntegration`. The only class in `src/main` that names a
  `com.maddyhome.idea.vim` type. Keep it that way.

Actions are registered in `src/main/resources/META-INF/plugin.xml`; the action IDs there are the
public API users bind in their `ideavimrc`, so renaming one breaks every existing config.

A keystroke becoming a file open, taking `<C-e>` then `<cr>` as the example:

1. IdeaVim (or an IDE keymap binding) invokes the `ShowHarpoon` action.
2. `ShowHarpoon` renders `HarpoonState.GetFiles` one canonical path per line, with the project base
   path abbreviated to `...`, and opens `HarpoonDialog` on that text.
3. `HarpoonDialog` builds an `EditorTextField` over a **real file on disk** (see the next section),
   asks `IdeaVimIntegration` to map `<cr>` to `:action SelectHarpoonItem`, and on the popup's first
   focus event sends Escape so it lands in normal mode with the caret on line 0.
4. `<cr>` fires `SelectHarpoonItem` → `ShowHarpoon.SelectHarpoonItem()` → `HarpoonDialog.Ok()`,
   which records the caret's line in `SelectedIndex` and closes the dialog.
5. `ShowHarpoon` re-reads the popup text through `dialog.getListText()` — the dialog caches it past
   `dispose()`, which is when the backing file is deleted. If the text changed, `SetFiles` rewrites
   the list. Then `NavigateToIndex` opens whatever is in `SelectedIndex`.

The round trip through the popup is lossy: `SetFiles` skips blank and unresolvable lines and only
advances its slot index on lines that resolve, so saving the popup re-packs the list and empty slots
do not survive it. Related to, but distinct from, the persistence gap listed below — that one loses
empty slots when writing to `PropertiesComponent`, this one loses them on the way in.

## The popup must stay backed by a real file

This is the single most expensive thing this project has learned. IdeaVim
[commit `2c057e93`](https://github.com/JetBrains/ideavim/commit/2c057e93) (VIM-3929, 2025-05-23),
first shipped in **IdeaVim 2.25.0 (published 2025-05-27)**, added an unconditional clause to
`EditorHelperRt.isIdeaVimDisabledHere`: IdeaVim now installs no key handling at all in an editor
whose document is not backed by a real file. Before 2.25.0 that check only applied through the
`ideavimsupport` option's `dialog` entry.

The Harpoon popup was an `EditorTextField` over a plain in-memory document, so from 2.25.0 on
IdeaVim attached nothing to it. Typed keys fell through to the platform's ordinary typing handler,
which is indistinguishable from being stuck in insert mode, and `VimShortcutKeyAction` was disabled
so Escape was never claimed by Vim and instead reached `DialogWrapper`'s cancel action. That is one
defect wearing two faces. It broke the plugin's headline feature for over a year, produced three
failed fix attempts in the git log (search for "normal mode") and user reports including
[issue #23](https://github.com/AlexGirardDev/HarpoonIJ/issues/23). Commit `6431caf` fixed it by
backing the popup's document with a real temp file.

What that means for anyone touching `HarpoonDialog`:

- **Never revert the popup to an in-memory document, and do not substitute a `LightVirtualFile`.**
  `EditorHelper.isFileEditor` explicitly rejects a `LightVirtualFile` unless it resolves to a
  non-light host file, i.e. unless it is an injected fragment. It has to be a real file on disk.
- **The temp file must stay randomised and owner-only.** `Files.createTempFile`, not
  `FileUtil.createTempFile`. A predictable path such as `/tmp/harpoon-list.txt` collides between two
  open projects and is a symlink-substitution hazard from another local user. This was caught and
  corrected during implementation; do not regress it.
- The file lives in the OS temp directory rather than the project, so it stays out of VCS, indexing
  and Local History, is saved on open so the IDE never asks about it changing on disk, and is
  deleted in `dispose()`. If it cannot be created the popup still opens — just not Vim-driven, which
  `forceNormalMode` logs.
- **Escape closing the popup from normal mode is correct**, not a bug. It is what the NeoVim
  original does. Do not "fix" it.
- `IdeaVimIntegration.isAttachedTo` calls IdeaVim's internal `EditorHelperRt.isIdeaVimDisabledHere`.
  It is guarded against `LinkageError`, but it is one more IdeaVim internal to re-verify whenever
  `ideaVimVersion` moves — alongside `KeyHandler.handleKey`, `MappingOwner`, and the `injector`
  accessors that class uses.

### `isAvailable()` versus `isAttachedTo()`

These answer different questions and the difference is load-bearing.

- `isAvailable()` — is the IdeaVim plugin loaded, i.e. are its classes on the classpath? IdeaVim is
  an *optional* dependency, so without it these types are simply absent. Every method in
  `IdeaVimIntegration` that touches a `com.maddyhome.idea.vim` class must check this itself, so
  callers can call unconditionally. This is a necessary condition, never a sufficient one.
- `isAttachedTo(editor)` — is IdeaVim actually handling keys in *this* editor? Ask this before doing
  anything editor-specific. `forceNormalMode` on an unattached editor was actively harmful: the Vim
  mode is application-global, so sending Escape reached through the dialog and changed the mode of
  the file editor behind it while doing nothing whatsoever for the popup.

## The testing bar

This suite once had sixteen tests specifically covering popup normal-mode behaviour. All sixteen
passed while the feature was completely broken in a real IDE.

The cause will recur, so state it plainly: **`VimEditor.getMode()` delegates to a single
application-global state machine.** It reports nothing about any particular editor. The mode value
was measured to be identical in the broken case and the working case. Those assertions were true and
carried zero information.

The resulting rules for `HarpoonPopupNormalModeTest` and `IdeaVimIntegrationTest`:

- Assert **observable behaviour in the editor under test** — what the document contains after a
  keystroke, where the caret moved — not a value read from a framework singleton. `j` must move the
  caret without changing the text; `dd` must delete a line.
- Keep the attach tripwire (`testIdeaVimDrivesThePopupsEditor`,
  `testIdeaVimDoesNotDriveAnInMemoryEditor`). It fails loudly if IdeaVim ever stops attaching to the
  popup, so this failure mode can never again be silent.
- **A new or changed test in this area must be demonstrated to fail against the broken behaviour
  before it is trusted.** Revert the fix locally, watch it go red, put the fix back. That step, not
  the green run, is what caught the real defect.
- The global Vim mode is shared test-to-test. One test leaving it dirty made an unrelated later test
  fail. Both classes call `injector.getVimState().reset()` in `setUp` and `tearDown`; tests here must
  stay order-independent.
- Asserting on IdeaVim's *mapping registry* is fine — unlike the mode, that is per-mapping state this
  plugin itself writes.
- Headless fixtures cannot cover Escape closing the dialog: that needs Swing's
  `WHEN_IN_FOCUSED_WINDOW` binding on a real focused, mapped window. Verify it in a sandbox IDE.

**Headless tests were proven insufficient here.** Any change to popup key handling or the IdeaVim
integration needs a real-IDE check as well; see "Verifying a real IDE behaviour change" in
`development.md`.

## Sharp edges

- **IdeaVim is the fragile part.** Optional dependency, internals that move between releases, and
  the source of every recurring defect in this plugin's history. When you bump `ideaVimVersion`,
  `IdeaVimIntegration` and `IdeaVimIntegrationTest` are what to check first.
- **`HarpoonState` is static state keyed by `project.getName()`**, not by project instance. Two open
  projects with the same name share one Harpoon list.
- **`.run/Run Plugin.run.xml` runs `publishPlugin`**, despite the name. Do not launch it to try the
  plugin locally; use `./gradlew runIde`.
- **Publishing is gated on a published GitHub Release, not on a merge.** `.github/workflows/`
  holds `release.yml`; nothing that lands on `master` reaches the Marketplace on its own. The
  release ritual, the signing secrets and the mandatory real-IDE check live in `development.md`
  under "Releasing" — never document or change release mechanics in two places.
- **`HarpoonJumpList` is an on-disk contract.** Changing that key silently drops every existing
  user's list. Same for the action IDs in `plugin.xml`.

## Known behaviour gaps (still open on `master`)

All four re-confirmed by test probe against the current code. Deliberately left unchanged pending a
maintainer decision — do not fix them as a side effect of unrelated work.

- **Slot positions do not survive a restart.** `HarpoonState.SetItem` persists a null-filtered list
  (the `filter(... != null && isValid())` before `setList`), so `[a, -, -, -, e]` comes back as
  `[a, e]` and "Goto Harpoon File 5" changes target. Measured: five slots in, two paths persisted,
  `[a.txt, e.txt]` back.
- **Clearing every entry never persists.** `HarpoonState.SetFiles` clears only the in-memory cache
  and then writes nothing when no entry resolves, so `PropertiesComponent` still holds the old list
  and a restart undoes the clear. Emptying the popup goes down this path: the single remaining blank
  line is skipped as blank. Measured: in-memory `[]`, persisted still both paths, both back after a
  reload.
- **Selecting a popup entry whose file was deleted mid-session throws.**
  `ShowHarpoon.NavigateToIndex` does not check `VirtualFile.isValid()` the way
  `GoToHarpoonActionBase.actionPerformed` does, so the stale `VirtualFile` reaches
  `FileEditorManager.openFile`, which blows up inside the platform. The exception type depends on
  the platform's editor-manager implementation (an NPE under the test fixture), so guard on
  `isValid()` rather than catching anything.
- **`HarpoonState.GetItem` throws `IndexOutOfBoundsException` for a negative index** rather than
  returning null: the guard is `index < Files.size()`, which a negative index passes. Note that
  `HarpoonDialog.SelectedIndex` starts at `-1`.

## Maintaining this file

Keep this file for knowledge useful to almost every future agent session in this project.
Do not repeat what the codebase already shows; point to the authoritative file or command instead.
Prefer rewriting or pruning existing entries over appending new ones.
When updating this file, preserve this bar for all agents and keep entries concise.
