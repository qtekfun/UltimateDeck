<!--
SPDX-FileCopyrightText: 2026 UltimateDeck contributors
SPDX-License-Identifier: GPL-3.0-or-later
-->

# Changelog

All notable changes are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow [Semantic Versioning](https://semver.org).

## [Unreleased]

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
