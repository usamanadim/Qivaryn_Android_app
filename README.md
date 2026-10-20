# Qivaryn

Qivaryn is a serverless, on-device Instagram media downloader built with Kotlin,
Jetpack Compose, and a Python download engine.

- **App repository / releases:** [`usamanadim/Qivaryn_Android_app`](https://github.com/usamanadim/Qivaryn_Android_app)
- **Engine library:** [`usamanadim/qivaryn-engine`](https://github.com/usamanadim/qivaryn-engine)

The app's in-app updater reads **this repository's** releases. The engine
repository publishes the Python package and CLI, which an installed Android app
cannot use as an update.

## Current release

The first public build is **`0.1.1-beta`** (version code 2), published as a
GitHub pre-release. Grab
`Qivaryn-0.1.1-beta-universal.apk` from the
[releases page](https://github.com/usamanadim/Qivaryn_Android_app/releases).
Release notes live in [`docs/release-notes/`](docs/release-notes).

## Download location

All successful downloads are published to the operating system's standard
Downloads directory:

```
Downloads/Qivaryn/
```

- Android 10+ uses MediaStore (`Download/Qivaryn`) so the folder works with
  scoped storage.
- Android 9 and below writes directly to the public Downloads directory after
  requesting storage permission.
- Existing private downloads from older versions are migrated into the public
  folder on first launch.
- Filename conflicts are resolved with `name (1).ext`, `name (2).ext`, etc.;
  existing files are never overwritten.
- Download history, thumbnails, file opening, and sharing all read from the
  public folder. Failed downloads are shown with a **Failed** status and are
  never added as completed media.

## Instagram download scopes

The Instagram screen offers:

- **Single** — Post, Reel, or Profile media selection. Story and Highlights
  are not supported by the engine yet.
- **All Reels** — downloads every reel available from a profile URL using the
  existing Python engine's account-Reels feed.
- **All Posts** — downloads posts, videos, and carousels from a profile URL
  using the engine's account feed, filtered to posts.

Bulk operations run on a background thread, disable duplicate execution while
active, and report completion or failure through status messages.

## Appearance

The Settings screen includes a persisted **Dark mode** switch. The complete UI
uses a centralized palette via `QivarynColors`; the selected mode is restored
on subsequent launches and applies immediately without restarting the app.

## Build

A local release build needs Python 3.11 (Chaquopy's build interpreter) and, to
be installable, a signing key supplied outside version control. Create a
`keystore.properties` in the project root (git-ignored):

```properties
storeFile=keystore/qivaryn-release.jks
storePassword=…
keyAlias=qivaryn
keyPassword=…
```

Then:

```powershell
.\gradlew.bat :app:assembleDebug     # unsigned debug build, no keystore needed
.\gradlew.bat :app:assembleRelease   # signed with keystore.properties
```

Outputs:

- debug: `app/build/outputs/apk/debug/app-debug.apk`
- release: `app/build/outputs/apk/release/app-release.apk`

Build a stable-only channel with `-Pqivaryn.updateChannel=stable`; the default
is `beta`.

## Updates

Settings → **Software update** checks GitHub Releases in this repository for a
newer build on the app's channel, shows the release notes, and installs it
through Android's package installer. Because the Python engine is packaged
inside the APK, an engine fix ships as a normal app update; the screen shows the
app build and the bundled engine version side by side.

- **Beta channel** (default): sees `-beta` releases and stable releases.
- **Stable channel**: sees stable releases only.

Installing over the same package with the same signing key is an upgrade, so
cookies, download history, settings, and `Downloads/Qivaryn` are preserved.

## Publishing a release

Tag `vMAJOR.MINOR.PATCH` (add `-beta` for a pre-release) that matches
`versionName` in `app/build.gradle` and push it. The
[`Release` workflow](.github/workflows/release.yml) builds the signed APK,
writes checksums, and creates the GitHub release automatically. This needs the
`QIVARYN_KEYSTORE_BASE64`, `QIVARYN_KEYSTORE_PASSWORD`, `QIVARYN_KEY_ALIAS`, and
`QIVARYN_KEY_PASSWORD` repository secrets.

See [`docs/UPDATES.md`](docs/UPDATES.md) for the mechanism, the security model,
the channel design, and the full release checklist.
