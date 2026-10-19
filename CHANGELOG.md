<!--
SPDX-FileCopyrightText: 2026 UltimateDeck contributors
SPDX-License-Identifier: GPL-3.0-or-later
-->

# Changelog

All notable changes are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow [Semantic Versioning](https://semver.org).

## [Unreleased]

## [1.0.0-rc.6] - 2026-10-19

Reach a server on your local network on Android 17.

### Fixed
- On Android 17 the app could not reach a server on your local network (for example a home server reached through split DNS), and every sync timed out. Now, only when your server is on the local network, the app explains why it needs the "Nearby devices" permission and asks for it.

## [1.0.0-rc.5] - 2026-10-11

Fifth release candidate: the card editor on wide screens.

### Changed

- Landscape and tablets: the card editor shows the card's fields and attachments on the left and the description with its toolbar on the right, each with its own scroll, so the description stays visible with the keyboard open.
- Settings are centred and capped in width on wide screens.

## [1.0.0-rc.4] - 2026-10-11

Fourth release candidate: one more keyboard fix.

### Fixed

- The card editor no longer leaves a black gap above the keyboard: the "Saved Markdown" bar now rides right above it and the description uses all the space left.

## [1.0.0-rc.3] - 2026-10-11

Third release candidate: editor fixes.

### Added

- Delete a whole table from the card editor with the new "Delete table" button.

### Fixed

- The editor is no longer squashed at the top with a keyboard-sized gap when the keyboard opens.
- Text typed right after a task checkbox no longer lands inside its brackets and breaks the task; tapping beside the box places the cursor instead of ticking it.

## [1.0.0-rc.2] - 2026-10-05

Second release candidate: reminders that reach phones that stop background apps.

### Added

- Reminders guide in Settings → Reminders: tells if notifications are blocked and opens your phone maker's auto-start screens (ColorOS/OxygenOS, Xiaomi, Huawei/Honor, vivo, Samsung), with the app info page as fallback; robust mode is recommended on those phones.
- Heartbeat: while a reminder is pending, a silent alarm every 30 minutes sets the reminders again and brings back missed ones, for phones that freeze the app.
- Real test reminder: a button sends a reminder through the same path as real ones a minute later and tells whether it arrived on time, late or not at all.
- Robust mode (optional, in Settings → Reminders): a foreground service keeps the app alive on phones that stop background apps, so reminders are not lost. It uses no network.
- Missed reminders: when the system stops the app and its alarms never ring, reminders of the last 24 hours are shown late as "Did not arrive on time" when the app starts, after a sync and when any reminder comes in. Can be turned off in Settings → Reminders; it is part of the settings backup.

## [1.0.0-rc.1] - 2026-10-03

First release candidate of 1.0.0.

### Added

- Sign in with Nextcloud's Login Flow v2; the app password is encrypted with the Android Keystore.
- Offline-first sync with Nextcloud Deck: local database, operation queue with retries and backoff, change detection with ETags, periodic sync, pull to refresh.
- Conflict handling: title and description conflicts are shown to choose a version; other fields keep the latest change.
- Boards: side-by-side columns, drag and drop, favorite board, side menu, archived cards with restore; create boards and columns, and delete them once allowed in Settings.
- Cards: create, edit in place (WYSIWYG Markdown description), move, archive, delete; due date and time, labels, assignees.
- Attachments: add from camera, gallery or files, background upload with retries, on-demand download, delete.
- Due date reminders with exact alarms and an optional alarm-clock mode.
- Settings: theme, AMOLED black, dynamic colors, language, account; export and restore them, optionally with the sessions sealed by a password.
- English and Spanish.

[Unreleased]: https://github.com/qtekfun/UltimateDeck/compare/v1.0.0-rc.2...HEAD
[1.0.0-rc.2]: https://github.com/qtekfun/UltimateDeck/compare/v1.0.0-rc.1...v1.0.0-rc.2
[1.0.0-rc.1]: https://github.com/qtekfun/UltimateDeck/releases/tag/v1.0.0-rc.1
