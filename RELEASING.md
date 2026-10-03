<!--
SPDX-FileCopyrightText: 2026 UltimateDeck contributors
SPDX-License-Identifier: GPL-3.0-or-later
-->

# Releasing

## Versions

- The version lives in one place: `appVersion` in `gradle.properties`, as SemVer (`1.2.3`), or `1.2.3-rc.N` for a release candidate.
- The Android version code is derived from it, never set by hand: `(MAJOR*10000 + MINOR*100 + PATCH) * 100 + N`, with `N = 99` for a final release. So `1.0.0-rc.1` is `1000001` and `1.0.0` is `1000099`: a final version always sorts after its release candidates, and nothing depends on dates or the machine (reproducible builds).
- Before 1.0.0 the app is `0.x`.

## Signing (one time)

Releases are signed with the project's own key, and the builds are reproducible: F-Droid builds the same source, checks that its APK matches the one published here and then ships ours. That way the app can be updated from F-Droid or GitHub interchangeably.

1. Create the key, and keep the file and passwords somewhere safe and **backed up**: if the key is lost, users would have to uninstall to update.
   ```sh
   keytool -genkeypair -v -keystore ultimatedeck-release.jks -alias ultimatedeck \
     -keyalg RSA -keysize 4096 -validity 10000
   ```
2. Add these secrets to the GitHub repository (Settings → Secrets and variables → Actions):
   - `UD_KEYSTORE_BASE64`: `base64 -w0 ultimatedeck-release.jks`
   - `UD_KEYSTORE_PASSWORD`, `UD_KEY_ALIAS` (`ultimatedeck`), `UD_KEY_PASSWORD`
3. For F-Droid, give them the certificate fingerprint (`AllowedAPKSigningKeys` in its metadata):
   ```sh
   keytool -list -v -keystore ultimatedeck-release.jks -alias ultimatedeck | grep SHA256
   ```

Without these variables, `./gradlew assembleRelease` builds an unsigned APK, which is what F-Droid does before comparing.

## Making a release

1. Move the `[Unreleased]` notes in `CHANGELOG.md` under `## [X.Y.Z] - YYYY-MM-DD`.
2. Set `appVersion=X.Y.Z` in `gradle.properties`.
3. Commit (`chore: release X.Y.Z`), merge to `master`, then tag and push the tag:
   ```sh
   git tag vX.Y.Z && git push origin vX.Y.Z
   ```
4. The **Release** workflow checks that the tag matches `appVersion`, runs `./gradlew check`, builds the signed APK and publishes a GitHub Release with the notes of that version. Release candidates (`-rc.N`) are marked as pre-releases.
5. F-Droid picks the new tag up by itself (`UpdateCheckMode: Tags`).
