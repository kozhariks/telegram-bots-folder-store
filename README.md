# Bots Folder — Mini App home screen

A bots-only chat folder can show a grid of Mini App icons instead of the classic dialog list.

* **Tap** → open bot chat  
* **Long-press** (iOS) / **long-press** (Android) → open Mini App  
* Unread badges on tiles  
* Toggle in settings (on by default)

Telegram concept for **iOS** (UIKit / AsyncDisplayKit) and **Android** (Java, Telegram-Android), built on top of the official sources — same packaging style as [Telegram Designers](https://t.me/designers) concept repos (feature files + integration patches).

## Downloads

| Platform | Build |
|---|---|
| **Android** | [tezam-bots-folder-android-afat-debug.apk](https://github.com/kozhariks/telegram-bots-folder-store/releases/download/v0.1.0-android/tezam-bots-folder-android-afat-debug.apk) — [Release page](https://github.com/kozhariks/telegram-bots-folder-store/releases/tag/v0.1.0-android) |
| **iOS** | No IPA yet (needs Mac / TestFlight). Source + patch are below. |

| Path | What |
|---|---|
| `ios/Feature/submodules/…` | New iOS files — copy over a Telegram-iOS checkout |
| `ios/Integration/telegram-ios.patch` | Hooks in existing Telegram-iOS files (`git apply`) |
| `android/Feature/TMessagesProj/…` | New Android files |
| `android/Integration/telegram-android.patch` | Hooks in existing Telegram-Android files (`git apply`) |
| `Media/` | Demo recordings (add `ios.mp4` / `android.mp4` when available) |

## Behaviour

* Folder must be **bots-only** (chat types → only Bots).
* **Settings → Appearance / Chat settings → Mini App home screen**.
* App list: recent Mini Apps first, then bots from the main dialog list.

## iOS

| File | What |
|---|---|
| `…/ChatListUI/Sources/BotsFolderStoreHelper.swift` | Detect bots-only filter |
| `…/ChatListUI/Sources/BotsFolderStoreNode.swift` | Grid UI, badges, tap / long-press |

Hooks (see patch): `ChatListContainerItemNode` (embed / show-hide), `ChatSettings` (`botsFolderStoreMode`), `ThemeSettingsController` (settings switch).

Base commit: `24638bc69bd083981b178dd1e7f921eebbf7fc46` (TelegramMessenger/Telegram-iOS shallow tip used for this port).

```sh
cd Telegram-iOS
git checkout 24638bc69bd083981b178dd1e7f921eebbf7fc46   # or rebase the patch onto a newer tip
cp -R /path/to/this-repo/ios/Feature/submodules/ submodules/
git apply /path/to/this-repo/ios/Integration/telegram-ios.patch
```

Build as usual (`build-system/Make/Make.py` or generated Xcode project). Use your own `api_id` / `api_hash` from [my.telegram.org](https://my.telegram.org/apps) in the build configuration JSON (do not commit secrets).

## Android

| File | What |
|---|---|
| `org/telegram/ui/Components/BotsFolderStoreView.java` | Grid UI, badges, tap / long-press |
| `org/telegram/ui/Components/BotsFolderViewHelper.java` | Prefs + bots-only filter detect |

Hooks (see patch): `DialogsActivity` (attach / padding / chrome sync), `ThemeActivity` (settings row).

Base commit: `f32ffeec763f0bdbf4bec25b52d654f77bff733d` (DrKLO/Telegram tip used for this port).

```sh
cd Telegram
git checkout f32ffeec763f0bdbf4bec25b52d654f77bff733d
cp -R /path/to/this-repo/android/Feature/TMessagesProj/ TMessagesProj/
git apply /path/to/this-repo/android/Integration/telegram-android.patch
```

Build as usual (Gradle). Set your own `APP_ID` / `APP_HASH` in `BuildVars` locally — not included in this repo.

## Notes

* Telegram sources are GPLv2; these files are meant to be used on top of them.
* Full Telegram-iOS / Telegram-Android trees are **not** mirrored here — only the feature delta (Designers-style).
* iOS IPA / Simulator still need macOS + Xcode; Android APK builds on Windows/Linux/macOS with the usual Android SDK.
* Demo videos for `@designers` submission: drop into `Media/` when recorded.

## Want your project featured?

If submitting to Telegram Designers: working code + demo video. Requirements: https://t.me/designers/266
