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


## Notes:

·	Icon packs: Glimpse Colour, Glimpse's own colourful icons, is built in, and icon packs made for Nova, ADW and similar launchers work too. A pack's own Apply button works with Glimpse.
·	Change any single icon from its menu: any icon from any installed pack (with search), or a picture from the phone. All apps, Search and Emergency can have their own icon too.
·	Calendar icons show today's date. Icons you use keep showing if you uninstall the pack.
·	Folders can show a small badge (music, games, work and more), in mono or colour.
·	Menu text size: make the text in Settings, menus, Help and pop-ups smaller or larger (Settings › Colours and themes › Menus and highlight).
·	Buttons and menus fit narrow screens: buttons move onto more lines when they no longer fit side by side.

Install: download glimpse-launcher-plus-1.0.16.apk, open it and allow installs from that app when Android asks. Updating over an earlier Glimpse Launcher Plus keeps your setup.
Signing certificate SHA-256: AA:46:88:60:F2:DC:48:2D:C9:B7:C3:90:69:80:BF:3E:C1:FF:42:79:88:D1:D9:D4:2B:D5:A6:CE:B4:4A:66:7F

APK SHA-256: 3aa271415f41bc334d6203b3d8fd13ea88742e831b1f8fdb33a3783bc94053b7
The Glimpse Colour icons are based on WithIcons by Evergrow (MIT licence).

