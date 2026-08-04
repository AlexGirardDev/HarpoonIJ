<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# HarpoonIJ Changelog

This file is the single source of truth for the plugin's change-notes. The
`org.jetbrains.changelog` Gradle plugin renders the section matching the current
`pluginVersion` -- or `[Unreleased]` when there is no such section -- into
`plugin.xml` at build time, so do not hand-write `<change-notes>` there.

## [Unreleased]

### Changed

- Updated to the IntelliJ Platform 2026.2 (IDEA 2026.2.0.1, since-build 262), building with the
  IntelliJ Platform Gradle Plugin 2.x instead of the retired Gradle IntelliJ Plugin 1.x.
- Updated the optional IdeaVim integration to IdeaVim 2.45.1.
- The Harpoon popup now opens on the first entry instead of the second, so pressing enter
  straight away jumps to harpoon slot 1 rather than slot 2.

### Fixed

- Fixed the Harpoon popup opening in insert mode, with escape closing it instead of
  returning to normal mode. Since IdeaVim 2.25.0 IdeaVim only drives editors backed by a
  real file, so it was not handling keys in the popup at all; the popup is now backed by
  one.

## [0.2.0] - 2024-11-26

### Added

- Actions to move to the next and previous entry in the Harpoon list.

### Fixed

- Fixed a focus bug in the Harpoon popup.

## [0.1.7] - 2024-02-14

### Fixed

- Fixed an issue where the enter key would not work on the modal popup.

## [0.1.6] - 2024-02-01

### Fixed

- Fixed forcing normal mode with the then-current version of the IdeaVim plugin.
- Fixed assorted edge-case errors and exceptions.

<!-- Releases before 0.1.6 predate this file; see the repository's GitHub releases
     and git history for their contents. -->
