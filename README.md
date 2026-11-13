# Bots Folder Store — Mini App home screen for a bots-only folder

A bots-only chat folder can show a **grid of Mini App icons** instead of the classic dialog list — like an app shelf for your bots.

* **Tap** a tile → open the bot chat  
* **Long-press** a tile → open the Mini App  
* Unread badges on tiles  
* Toggle in settings (on by default)

Telegram concept for **iOS** (UIKit / AsyncDisplayKit) and **Android** (Java), built on top of the official sources — same packaging style as concepts sent to [@designers](https://t.me/designers) (feature files + integration patches, not a full client fork).

| Android | iOS |
|---|---|
| [Media/android.mp4](Media/android.mp4) *(add recording)* | [Media/ios.mp4](Media/ios.mp4) *(add recording)* |

## Downloads

Ready-to-try builds (sideload / install — not App Store / Play Store):

| Platform | File | Release |
|---|---|---|
| **Android** | [tezam-bots-folder-android-afat-debug.apk](https://github.com/kozhariks/telegram-bots-folder-store/releases/download/v0.1.0-android/tezam-bots-folder-android-afat-debug.apk) | [v0.1.0-android](https://github.com/kozhariks/telegram-bots-folder-store/releases/tag/v0.1.0-android) |
| **iOS** | [Telegram-bots-folder.ipa](https://github.com/kozhariks/telegram-bots-folder-store/releases/download/v0.1.0-ios-22/Telegram-bots-folder.ipa) *(unsigned debug, ~623 MB)* | [v0.1.0-ios-22](https://github.com/kozhariks/telegram-bots-folder-store/releases/tag/v0.1.0-ios-22) |

**All releases:** https://github.com/kozhariks/telegram-bots-folder-store/releases  

iOS: install with [Sideloadly](https://sideloadly.io/) / AltStore / your Apple Developer resign. The IPA is **fake-codesigned** (same idea as official Telegram-iOS CI unsigned builds).

## Behaviour

* Folder must be **bots-only** (chat types → only Bots).
* **Settings → Appearance / Chat settings → Mini App home screen**.
* App list: recent Mini Apps first, then bots from the main dialog list.

## Repository

| Path | What |
|---|---|
| `ios/Feature/submodules/…` | New iOS files — same paths as in Telegram-iOS; copy over a checkout |
| `ios/Integration/telegram-ios.patch` | Hooks in existing Telegram-iOS files (`git apply`) |
| `android/Feature/TMessagesProj/…` | New Android files |
| `android/Integration/telegram-android.patch` | Hooks in existing Telegram-Android files (`git apply`) |
| `Media/` | Demo recordings for [@designers](https://t.me/designers) (`ios.mp4` / `android.mp4`) |
| `.github/workflows/` | CI: Android APK + iOS IPA |

## iOS

| File | What |
|---|---|
| `ChatListUI/Sources/BotsFolderStoreHelper.swift` | Detect bots-only filter |
| `ChatListUI/Sources/BotsFolderStoreNode.swift` | Grid UI, badges, tap / long-press |

Hooks (see patch): `ChatListContainerItemNode` (embed / show-hide), `ChatSettings` (`botsFolderStoreMode`), `ThemeSettingsController` (settings switch).

Base commit: `24638bc69bd083981b178dd1e7f921eebbf7fc46` (TelegramMessenger/Telegram-iOS).

```sh
cd Telegram-iOS
git checkout 24638bc69bd083981b178dd1e7f921eebbf7fc46
cp -R /path/to/this-repo/ios/Feature/submodules/ submodules/
git apply /path/to/this-repo/ios/Integration/telegram-ios.patch
```

Build as usual (`build-system/Make/Make.py` or the generated Xcode project). Use your own `api_id` / `api_hash` from [my.telegram.org](https://my.telegram.org/apps) in the build configuration JSON — do not commit secrets.

CI produces an unsigned **debug** IPA on Xcode 26.6 (`debug_arm64`); tip release opts need Xcode 27.1.

## Android

| File | What |
|---|---|
| `org/telegram/ui/Components/BotsFolderStoreView.java` | Grid UI, badges, tap / long-press |
| `org/telegram/ui/Components/BotsFolderViewHelper.java` | Prefs + bots-only filter detect |

Hooks (see patch): `DialogsActivity` (attach / padding / chrome sync), `ThemeActivity` (settings row).

Base commit: `f32ffeec763f0bdbf4bec25b52d654f77bff733d` (DrKLO/Telegram).

```sh
cd Telegram
git checkout f32ffeec763f0bdbf4bec25b52d654f77bff733d
cp -R /path/to/this-repo/android/Feature/TMessagesProj/ TMessagesProj/
git apply /path/to/this-repo/android/Integration/telegram-android.patch
```

Build as usual (Gradle). Set your own `APP_ID` / `APP_HASH` in `BuildVars` locally — not included in this repo.

## Notes

* Telegram sources are GPLv2 / upstream terms; these files are meant to be used **on top of** them.
* Full Telegram-iOS / Telegram-Android trees are **not** mirrored here — only the feature delta ([@designers](https://t.me/designers)-style).
* Demo videos: drop `Media/ios.mp4` and `Media/android.mp4` when recorded (1920×1920, 60 fps is the usual designers format).

## Want your project featured?

Working code + a video of the feature in action → [@designers](https://t.me/designers).  
Requirements: https://t.me/designers/266
