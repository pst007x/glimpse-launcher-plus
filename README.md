# Glimpse Launcher Plus

Glimpse Launcher Plus is the [Glimpse Launcher](https://glimpse-media.com/glimpse/) home screen for Android with
extras that Google Play does not allow in a launcher. It is offered outside Google Play. Everything else matches the
Play edition, and both are built from the same code.

## What Plus adds

- **All files and folders** in Search (Settings > Home screen > Search): find any file or folder in the phone's own
  storage by its name, as you type, and open it. Memory cards, hidden files, and Android/data and Android/obb are left
  out. It needs Android's All files access, which you allow yourself on Android's own page for the app.

Only names are read, on the phone. Plus never reads what is inside a file, never changes or deletes anything, and
has no internet permission. Privacy policy: https://glimpse-media.com/glimpse/privacy.html

## What's new

- **1.0.18:** Screen wallpapers can be set up ahead: + Add another screen is always there, even for screens you have
  not made yet. A screen shows its picture as soon as you make it.
- **1.0.17:** Big icons: make any app, folder, All apps, Search or Emergency 2 × 2 (a big folder shows its apps,
  ready to open). A different wallpaper for each screen, and place any wallpaper picture yourself (drag, pinch,
  Centre). Import icon packs finds packs that are not listed; Apply the pack to every icon clears single changes.
- **1.0.16:** Icon packs: Glimpse Colour (Glimpse's own colourful icons) is built in, and Nova-style packs work too.
  Change any single icon, even to a picture; calendar icons show the date. Folder badges, and menu text size.

Each version's full notes are on its [release page](https://github.com/pst007x/glimpse-launcher-plus/releases).

## Download

The newest version, with install steps: https://glimpse-media.com/glimpse/plus

Each version's APK and its SHA-256 checksum are also under this repository's
[Releases](https://github.com/pst007x/glimpse-launcher-plus/releases).

The APK is signed with the Glimpse Launcher Plus key. Its SHA-256 certificate fingerprint:

    AA:46:88:60:F2:DC:48:2D:C9:B7:C3:90:69:80:BF:3E:C1:FF:42:79:88:D1:D9:D4:2B:D5:A6:CE:B4:4A:66:7F

Each release lists the APK's SHA-256 checksum next to it.

## Building from source

Android SDK 36 and JDK 17 (signing and release steps: `docs/release/README.md`):

    ./gradlew testPlusDebugUnitTest assemblePlusRelease

The Plus edition's own code is in `app/src/plus/`; the Play edition is `./gradlew assemblePlayRelease`.
A release build without your own `plus-keystore.properties` (see `docs/release/README.md`) is unsigned.

## How it was made

Glimpse Launcher was designed by me, Salvatore Turone, and written with the help of
[Claude Code](https://claude.com/claude-code), Anthropic's AI coding tool. The ideas are mine: I decided
what each feature does and how it looks, tested every release on my own phone, and asked for
changes until it worked the way I wanted. Claude wrote most of the code. I see it as a tool, like a
photo filter or a compiler: it let me build apps I couldn't have written on my own. Commits made with
it carry a `Co-Authored-By: Claude` line.

## Licence

Copyright © 2026 Glimpse Media (Salvatore Turone). Licensed under the GNU General Public License, version 3 (see
`LICENSE`).

## Contact

support@glimpse-media.com
