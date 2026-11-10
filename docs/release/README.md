# Releasing Glimpse Launcher

Current version: **1.0.13** (versionCode 14). Two editions from one project: **Play** (`com.glimpsemedia.glimpselauncher`, on Google Play) and **Plus** (`com.glimpsemedia.glimpselauncherplus`, never on Play: it adds All files and folders search, which needs All files access). Both: minimum Android 8, built for Android 16 (API 36).

Glimpse Launcher began as a fork of Lighthouse Launcher (from its 1.0.7 source). It is a separate app with its own package, signing key and Play listing.

## The signing key: create it, then back it up

The upload key lives **outside** the project, in `~/glimpse-launcher-keys/`. You create it (Claude never sees the password):

```
mkdir -p ~/glimpse-launcher-keys
~/jdk/bin/keytool -genkeypair -v -keystore ~/glimpse-launcher-keys/glimpse-upload.jks -alias glimpse-upload -keyalg RSA -keysize 4096 -validity 10000
```

Then write `~/glimpse-launcher-keys/keystore.properties`:

```
storeFile=/home/<you>/glimpse-launcher-keys/glimpse-upload.jks
storePassword=...
keyAlias=glimpse-upload
keyPassword=...
```

Copy the folder to a safe place that is not this computer. The Gradle build reads `keystore.properties` automatically; without it the release build is unsigned.

### The Plus key

Glimpse Launcher Plus is signed with its own key, never the Play one. Create it the same way (you choose the password):

```
~/jdk/bin/keytool -genkeypair -v -keystore ~/glimpse-launcher-keys/glimpse-plus.jks -alias glimpse-plus -keyalg RSA -keysize 4096 -validity 10000
```

Then write `~/glimpse-launcher-keys/plus-keystore.properties`:

```
storeFile=/home/<you>/glimpse-launcher-keys/glimpse-plus.jks
storePassword=...
keyAlias=glimpse-plus
keyPassword=...
```

Back it up with the Play key: every Plus update must be signed with this same key, or phones refuse it.

## Building

```
export JAVA_HOME=~/jdk ANDROID_HOME=~/android-sdk
./gradlew testPlayDebugUnitTest testPlusDebugUnitTest assemblePlayRelease bundlePlayRelease assemblePlusRelease
```

- `app/build/outputs/bundle/playRelease/app-play-release.aab`: signed bundle for Google Play
- `app/build/outputs/apk/play/release/app-play-release.apk`: the Play edition as an APK
- `app/build/outputs/apk/plus/release/app-plus-release.apk`: Glimpse Launcher Plus, signed with the Plus key

Copy them to `release-<version>/` as `glimpse-launcher-<version>.aab`, `glimpse-launcher-<version>.apk` and
`glimpse-launcher-plus-<version>.apk`, with `sha256sum glimpse-launcher-plus-<version>.apk > glimpse-launcher-plus-<version>.apk.sha256`.

Checks before uploading:
- The Play build fails by itself if its manifest ever has a storage permission. To see it for yourself:
  `aapt2 dump permissions app/build/outputs/apk/play/release/app-play-release.apk` lists no `…EXTERNAL_STORAGE`.
- `keytool -printcert -jarfile` on the `.aab` shows the upload key (SHA-256 72:2C:51:0B…89:0B);
  `apksigner verify --print-certs` on the Plus APK shows the Plus key, not that one.

`docs/release/graphics/make_graphics.py` (run from that folder) rebuilds the Play icon and feature graphic from the screenshots.

## Releasing Glimpse Launcher Plus (GitHub, and F-Droid later)

Plus is built from the public GitHub copy, so the APK we publish is exactly what F-Droid builds from the same tag
(reproducible builds: F-Droid then ships our APK, signed with the Plus key).

1. Raise `versionCode`/`versionName`, write `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` (500
   characters at most), commit.
2. `docs/release/plus/export_github.sh` makes the GitHub copy's commit and tag `v<versionName>` (it refuses a tag that
   exists, binaries other than the Gradle wrapper and pictures, and listing files that break F-Droid's limits).
3. `docs/release/plus/build_plus_release.sh` builds the APK twice in the GitHub copy and writes
   `release-<v>/glimpse-launcher-plus-<v>.apk` and `.sha256` only when both builds are the same, clean and Plus-signed.
4. You: `git -C ~/glimpse-launcher-plus-github push --follow-tags`, then the GitHub release with the APK and its
   `.sha256` (the APK's file name must stay `glimpse-launcher-plus-<v>.apk`: F-Droid looks for it there).

`docs/release/plus/test_release_scripts.sh --build` tests these scripts in a scratch copy.

## Putting it on Google Play (done by you in Play Console)

1. Create app: name "Glimpse Launcher", English (United Kingdom), App, Free.
2. Publish `website/glimpse/` on glimpse-media.com as `/glimpse/` (the privacy policy address Play asks for), and add a link to it on the site's home page and menu.
3. Set up the app: the declarations in `data-safety.md` and `permissions.md`.
4. Store listing: texts, icon, feature graphic and screenshots from `store-listing.md`.
5. Testing > Internal testing: create a release, accept Play App Signing, upload the `.aab`, add yourself as a tester.
6. Closed test if your account needs it (12 testers, 14 days), then Production.

## Next versions

Raise `versionCode` (it must go up with every upload) and `versionName` in `app/build.gradle.kts`.

## Trying a change without touching the real app on the phone

`./gradlew assemblePlayPreview` builds "Glimpse test", a second copy with its own data (`com.glimpsemedia.glimpselauncher.preview`);
`./gradlew assemblePlusPreview` builds "Glimpse Plus test" (`com.glimpsemedia.glimpselauncherplus.preview`).
`./gradlew assemblePlayRelease -PidSuffix=.rctest` builds a release (R8) build that can sit next to the real one.
