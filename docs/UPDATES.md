# Qivaryn updates: mechanism and release process

This document explains how the in-app updater works, why it updates the way it
does, and what to do for every future release so the app picks it up
automatically.

The app has its **own** release repository:

| Purpose | Repository |
| --- | --- |
| Android app releases (what the in-app updater installs) | [`usamanadim/Qivaryn_Android_app`](https://github.com/usamanadim/Qivaryn_Android_app) |
| Python engine library / CLI (`.whl`, `.tar.gz`) | [`usamanadim/qivaryn-engine`](https://github.com/usamanadim/qivaryn-engine) |

`UPDATE_REPO` is compiled into the APK and points at the app repository. The
engine repository is a different distribution channel and is never used as an
APK feed.

---

## 1. Architecture analysis (why the update unit is the APK)

An update design has to follow what the app actually is, not what would be
convenient.

| Fact about this project | Consequence for updates |
| --- | --- |
| The Python engine lives in `app/src/main/python/` and is packaged into the APK by Chaquopy as read-only assets. | Engine code cannot be edited in place at runtime. |
| The engine depends on pip packages (`requests`, `tqdm`) bundled at build time by Chaquopy. | A "drop in a new `.py` file" update could not change dependencies. |
| Video muxing uses `libffmpeg.so` per ABI in `jniLibs/`. | Native binaries can only be replaced by installing a package. |
| The engine calls into Kotlin (`bridge.isPublished`, `bridge.mergeVideoAudio`, `bridge.onProgress`). | An engine update that needs a new bridge hook also needs new Kotlin. |
| Engine state (`cookies.txt`, `qivaryn_state.db`) lives in `filesDir`, outside the APK. | It must survive updates — and it does. |

**Conclusion:** the engine is not independently deployable. It ships *inside* the
APK, so an engine fix, a dependency bump, or a bridge change all arrive as a new
**app** release. The updater therefore updates the APK, and the two versions are
surfaced separately in the UI (`Engine 0.1.0 · beta channel · build 3`) so it is
always clear which engine a build carries.

### Rejected alternative: hot-swapping Python modules

Chaquopy searches `sys.path`, and `chaquopy_setup.py` already appends a writable
directory, so it is technically possible to shadow `downloader.py` with a
downloaded copy. This was rejected because it would:

- fail for any change that touches pip packages or native libraries;
- break the Kotlin↔Python bridge contract with no compile-time signal;
- execute arbitrary downloaded code with the app's full permissions — remote
  code execution as an update mechanism;
- create a second, drifting copy of the engine that is impossible to reason about.

The APK path is strictly safer and no harder for the user.

---

## 2. Release channels

The channel is compiled into the APK as `UPDATE_CHANNEL` (`app/build.gradle`),
next to the repository, for the same reason: a feed that could be retargeted at
runtime would let anyone redirect users to a foreign APK.

| Channel | `UPDATE_CHANNEL` | Sees | Typical tag |
| --- | --- | --- | --- |
| Beta (this build) | `beta` | beta releases **and** stable releases | `v0.1.0`, `v0.2.0-beta.1` |
| Stable (future GA) | `stable` | stable releases only | `v1.0.0` |

The `0.x` line is the pre-1.0 beta line: the app targets a single platform today,
and `1.0.0` is reserved for the feature-complete, all-platforms release. Every
`0.x` tag is published as a pre-release, so a stable-channel build never sees it.

A beta build following beta *and* stable is intentional: a tester on a `0.x`
build must still be able to move to `1.0.0` once the app is feature-complete.
SemVer ordering already prevents a `1.0.0-beta` from being offered to someone
already on `1.0.0`.

To build a stable-only app:

```powershell
.\gradlew.bat :app:assembleRelease -Pqivaryn.updateChannel=stable
```

A `-beta` suffix is still understood for releases between `0.x` builds, and the
client treats any release GitHub marks as a pre-release as a beta even if the tag
has no suffix, so a stable build never sees one by accident.

---

## 3. What the user sees

Settings → **Software update** (`UpdateScreen`).

| Situation | Shown |
| --- | --- |
| First open | Automatic check against GitHub |
| Already newest | "You're up to date" + version + channel |
| Newer release | Version, size, release notes, **Download and install** |
| Downloading | Progress bar, bytes, percentage |
| Verifying | "Verifying the update…" (checksum + signature + package checks) |
| Permission missing | **Open install settings** deep-link, then the install continues |
| Installing | System installer UI |
| Success | "Update installed" — app data intact |
| Any failure | Stage-specific title, message, advice, and a retry button |

The screen never blocks the rest of the app: every state is recoverable and the
old build remains fully usable.

---

## 4. How the update flow works

```
UpdateScreen
   │  LaunchedEffect
   ▼
UpdateManager.check()                     UpdateManager.downloadAndInstall()
   │                                              │
   ├─ GET /repos/{UPDATE_REPO}/releases           ├─ download APK → cacheDir/updates/*.part
   ├─ skip drafts                                 ├─ verify SHA-256 (if published)
   ├─ keep tags accepted by UPDATE_CHANNEL        ├─ UpdateInstaller.validate()
   ├─ keep releases with a compatible APK asset   │    package name / versionCode / minSdk
   ├─ pick highest SemVer                         │    signing certificate
   └─ compare with installed versionName          └─ PackageInstaller session → commit()
                                                          │
                                                          ▼
                                              InstallResultReceiver
                                              STATUS_PENDING_USER_ACTION → installer UI
                                              STATUS_SUCCESS / STATUS_FAILURE_* → UI state
```

### Version comparison

- The **installed** version comes from `PackageManager`, never from `BuildConfig`,
  so it always describes the package actually on the device.
- The **engine** version shown next to it is captured at build time from
  `app/src/main/python/__init__.py` and compiled in as `ENGINE_VERSION`. The
  engine is baked into the APK, so a runtime Python lookup would add no accuracy
  and one more way to fail.
- Released versions come from the tag (`v1.0.0` → `1.0.0`,
  `v0.2.0-beta.1` → `0.2.0-beta.1`). `SemVer.parse` rejects anything it cannot
  fully understand, so a stray tag can never be treated as a newer build.
- Prereleases sort below their stable counterpart (`0.2.0-beta.1 < 0.2.0`) while
  still sorting above an older stable release (`0.2.0-beta > 0.1.0`).
- After download, `versionCode` is compared again. A release tagged newer whose
  APK has a lower `versionCode` is refused — this also blocks downgrade attacks.

### Asset selection

Among a release's assets, the updater keeps `*.apk` files only, then ranks them:

1. an APK built for an ABI this device runs (device ABI order decides ties);
2. a `universal` APK;
3. an APK with no ABI in its filename (assumed universal).

Debug and unsigned builds are rejected — they can never replace a release build
anyway, and accepting them would only produce confusing installer errors. APKs
for an ABI the device does not support are rejected.

### Verification

| Check | Where | Failure behaviour |
| --- | --- | --- |
| TLS to GitHub | every request | check fails with "Could not reach GitHub" |
| SHA-256 of the APK | `UpdateManager.verifyIntegrity` | file deleted, "Update rejected" |
| Package name | `UpdateInstaller.validate` | refused |
| `versionCode` newer than installed | `UpdateInstaller.validate` | refused (no downgrade) |
| `minSdkVersion` ≤ device API | `UpdateInstaller.validate` | refused |
| Signing certificate matches installed app | `UpdateInstaller.validate` | refused |
| Signature enforced again by Android | system installer | install fails, app unchanged |

The SHA-256 is read from the release asset's `digest` field when GitHub provides
one, otherwise from a `*.apk.sha256`, `checksums.txt`, `SHA256SUMS` (or similar)
asset in the same release. A release with **no** checksum is still installable —
the signature comparison is the actual security boundary; the checksum only
catches a corrupted transfer. The release workflow publishes `checksums.txt`
automatically.

### Safety of user data

Installing over the same package name with the same signing key is an **upgrade**
to Android, not a replacement. The OS keeps:

- `filesDir` — `cookies.txt`, `qivaryn_state.db`
- `SharedPreferences` — dark mode, onboarding, suppressed history keys
- `Downloads/Qivaryn` — media files (outside app storage entirely)

The updater itself writes only to `cacheDir/updates/`, deletes that folder before
and after each attempt, and never touches app data or downloads.

> **The signing key is what makes this safe.** A build signed with a different key
> cannot be installed over the existing app. Users would have to uninstall first,
> which *does* delete cookies and history. Never lose or rotate the release
> keystore.

---

## 5. Security model in one paragraph

The update source (`UPDATE_REPO`) and the channel (`UPDATE_CHANNEL`) are compiled
into the APK, not stored in preferences, so neither the user nor another app on
the device can point Qivaryn at a foreign release feed. Downloads use HTTPS only,
with no cleartext traffic allowed. The artifact is bound to the release by its
published SHA-256 and bound to *this application* by package name, version code,
and signing certificate. Finally, Android re-verifies the signature and refuses
any APK not signed by the key that signed the installed app. A malicious release
page can therefore cause a failed update, but not a hostile one.

A GitHub token can be compiled into **debug** builds only
(`qivaryn.githubToken` in `gradle.properties`) to read a private repository.
Release builds always ship an empty token.

---

## 6. Release process

Do this for every release. The updater needs no code changes to pick it up.

### 6.1 Bump the version

In `app/build.gradle`:

```groovy
versionCode 4           // MUST increase by at least 1 on every release
versionName "0.2.0"     // MUST be MAJOR.MINOR.PATCH, optionally with -beta
```

Stay on the `0.x` line until every planned platform is implemented; `1.0.0` is
the feature-complete release. A `-beta` suffix is only needed for an interim
prerelease inside the `0.x` line; `0.x` tags are already published as
pre-releases.

`versionCode` is the authoritative monotonic counter Android uses to decide what
is an upgrade. `versionName` is what users and the update screen display. If
`versionName` is not `MAJOR.MINOR.PATCH`, the pre-download comparison cannot
order releases; the post-download `versionCode` guard still protects the install.

If the bundled engine changed, also bump `__version__` in
`app/src/main/python/__init__.py` so the Update screen reports the new engine
version.

> Tags must match `versionName`, because the comparison is *release tag vs
> installed `versionName`*. Tag `v0.2.0`, not `v0.2`.

### 6.2 Build and sign

Signing is read from outside version control:

- locally: `keystore.properties` in the project root (git-ignored), or
- in CI: the `QIVARYN_KEYSTORE_PATH`, `QIVARYN_KEYSTORE_PASSWORD`,
  `QIVARYN_KEY_ALIAS`, and `QIVARYN_KEY_PASSWORD` environment variables.

```powershell
.\gradlew.bat :app:assembleRelease
```

Sign with the **same keystore as every previous release**. Keep the keystore and
its passwords backed up and out of the repository. `keystore.properties` looks
like:

```properties
storeFile=keystore/qivaryn-release.jks
storePassword=…
keyAlias=qivaryn
keyPassword=…
```

### 6.3 Tag and publish

The recommended path is the GitHub Actions workflow
(`.github/workflows/release.yml`). It builds the signed APK on a clean runner,
writes `checksums.txt`, and creates the GitHub release. Push a matching tag:

```powershell
git tag v0.2.0
git push origin v0.2.0
```

Pre-1.0 tags (`v0.x.y`) and tags containing `-` are published as GitHub
**pre-releases**; `v1.0.0` and later are published as stable releases.

The workflow needs these repository secrets once:

| Secret | Value |
| --- | --- |
| `QIVARYN_KEYSTORE_BASE64` | Base64 of the release keystore (see 6.4) |
| `QIVARYN_KEYSTORE_PASSWORD` | keystore password |
| `QIVARYN_KEY_ALIAS` | `qivaryn` (or your alias) |
| `QIVARYN_KEY_PASSWORD` | key password |

### 6.4 Manual publishing (fallback)

1. Create a release from the tag. Mark `0.x` and `-beta` releases as a
   **pre-release**; leave `1.0.0`+ stable releases as-is. Drafts are ignored by
   the app.
2. Attach the signed APK, e.g. `Qivaryn-0.1.0-universal.apk`.
   - Include an ABI in the filename (`arm64-v8a`, `armeabi-v7a`, `x86_64`) or use
     `universal`. Do **not** put `debug` or `unsigned` in the name.
3. Write release notes in the release body — they are shown verbatim in the app.
   `docs/release-notes/v0.1.0.md` is the template used by CI.
4. Attach a checksum:
   ```powershell
   Get-FileHash .\Qivaryn-0.1.0-universal.apk -Algorithm SHA256
   ```
   either as `Qivaryn-0.1.0-universal.apk.sha256` or as a `checksums.txt` line.

Create the base64 blob for the `QIVARYN_KEYSTORE_BASE64` secret with:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("keystore\qivaryn-release.jks")) | Set-Clipboard
```

The next time a user opens **Software update**, the release appears automatically.

### 6.5 Pointing at a different repository

Change one line in `app/build.gradle`:

```groovy
def updateRepo = "owner/repository"
```

Releases must then be published there. Nothing else changes — the updater reads
whatever repository publishes the release.

---

## 7. Engine releases vs app releases

These are two different distribution channels and only one of them can update an
installed app:

| Channel | What is published | Can the in-app updater use it? |
| --- | --- | --- |
| `qivaryn-engine` releases | `qivaryn_engine-x.y.z-py3-none-any.whl`, `.tar.gz` | **No.** A running Android app cannot install a Python wheel; its engine is already compiled into the APK. |
| `Qivaryn_Android_app` releases | a signed `Qivaryn-*-universal.apk` | **Yes** — this is what the updater installs. |

The app repository is now the only thing `UPDATE_REPO` points at. If a release
there ever carries no `.apk`, the Update screen reports that honestly instead of
offering a broken install.

> **Tag with the app version, not the engine version.** The comparison is
> *release tag vs installed `versionName`*. Mention the bundled engine version in
> the release notes instead.

---

## 8. Troubleshooting

| Symptom | Cause | Fix |
| --- | --- | --- |
| "does not include an Android APK" | The newest accepted release has no `.apk` asset | Attach the signed APK to the release |
| "no stable/beta release published … yet" | All releases are drafts, the wrong channel, or none exist | Publish a release the channel accepts |
| Beta release not offered | `UPDATE_CHANNEL` is `stable`, or the tag has no prerelease and the version is not newer | Build with the beta channel / check the version bump |
| Update offered but install fails with `FAILURE_CONFLICT` | Installed build signed with a different key | Install the correctly signed build |
| Update never appears | `versionCode`/`versionName` not bumped, or tag does not match `versionName` | Fix the tag or version and re-release |
| "Not signed with the same key" | Release APK signed with a different keystore | Re-sign with the release keystore |
| "Already up to date" on a newer release | Tag parses lower than the installed name, or the channel filters it out | Correct the tag / channel |
| HTTP 404 during check | Repository private, renamed, or misspelled | Fix `UPDATE_REPO`, or use a debug build with `qivaryn.githubToken` |
| HTTP 403 during check | Unauthenticated GitHub rate limit (60/hour) | Wait, or use a token in a debug build |
