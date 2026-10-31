# Easy Launcher

A simple, large-button home-screen replacement for Android, designed for an
elderly user who finds normal phone/tablet interfaces fiddly. It runs best in
**landscape** and shows **6 or 8 big, colourful buttons** that each launch one
app or function.

Built for a Samsung Galaxy A11 tablet on Android, but works on any Android
phone/tablet running **Android 8.0 (Oreo) or newer**.

## What it does

- Replaces the home screen with a grid of 6 or 8 large buttons.
- Built-in colourful icons for **Phone, WhatsApp, Camera, Photos and Contacts**.
- Any other app (e.g. a game) can be assigned to a button and uses that app's
  own icon — or you can give it the built-in colourful **Game** icon.
- Each button has a big icon and a clear label.
- Phone, Camera, Contacts and Photos use the standard Android actions, so they
  work regardless of which dialler/camera/gallery the tablet uses.
- A **PIN-protected settings screen** (so your relative can't change things by
  accident). Reach it via the small gear in the top-right corner, *or* by
  holding a finger anywhere on the home screen. Either way the PIN is required.
- The **Back button does nothing** on the home screen, so she can't get "lost".

## First run

1. Install and open the app (or set it as the Home app — see below).
2. Open settings (gear / long-press). The first time, you'll be asked to
   **create a PIN**. Remember it — you'll need it to change settings later.
3. Configure the 6/8 buttons, then tap **Set Easy Launcher as the Home app** and
   choose Easy Launcher (set it as the default / always).

## Configuring a button

In settings, tap **Edit** on a button, then either:
- pick a **Quick choice** (Phone, WhatsApp, Camera, Photos, Contacts), or
- tap **Choose an installed app…** to pick any other app (e.g. a game).

You can change the **label** and choose the **icon** (the app's own icon or the
colourful Game icon). Tap **Save**. **Clear this button** empties a slot.

---

## Building it (Android Studio on Windows)

This is a standard Gradle/Android Studio project.

1. Install **Android Studio** (latest stable). On first run let it install the
   Android SDK.
2. **File → Open** and select the `C:\work\Claude\EasyLauncher` folder.
3. Android Studio will read `gradle/wrapper/gradle-wrapper.properties`, download
   Gradle 8.13 and sync. If it offers to set up / download the Gradle wrapper,
   accept. (If you build from the command line instead, first run
   `gradle wrapper` once in the project folder to generate the wrapper files.)
4. Let the Gradle sync finish (it downloads the Android Gradle Plugin and
   AndroidX libraries the first time).

> This project has already been built once on this machine — a ready-to-install
> debug APK is at `app/build/outputs/apk/debug/app-debug.apk`. It was built with
> AGP 8.13.2 / Gradle 8.13 / Kotlin 2.0.0 against your installed SDK 35.
> `local.properties` already points at your Android SDK.

### Install on the tablet

1. On the tablet: **Settings → About tablet → Software information** and tap
   **Build number** 7 times to enable Developer options.
2. **Settings → Developer options → USB debugging: ON**.
3. Connect the tablet to the PC with a USB cable; approve the debugging prompt
   on the tablet.
4. In Android Studio, pick the tablet from the device dropdown and press **Run**
   (the green ▶). The app installs and launches.

To build a shareable APK instead: **Build → Build Bundle(s) / APK(s) → Build
APK(s)**. The debug APK is written to
`app/build/outputs/apk/debug/app-debug.apk`; copy it to the tablet and open it
to install (allow "install from unknown sources" if prompted).

### Make it the home screen

After installing, press the tablet's Home button once — Android asks which app
to use as Home; choose **Easy Launcher** and **Always**. (You can also use the
**Set Easy Launcher as the Home app** button inside the app's settings.)

To go back to the normal launcher later: the tablet's **Settings → Apps →
Default apps → Home app** (path varies slightly by Android version).

## Project layout

```
EasyLauncher/
├─ build.gradle.kts, settings.gradle.kts, gradle.properties
├─ gradle/wrapper/gradle-wrapper.properties
└─ app/
   ├─ build.gradle.kts
   └─ src/main/
      ├─ AndroidManifest.xml
      ├─ java/com/example/easylauncher/
      │  ├─ MainActivity.kt        # the home grid
      │  ├─ SettingsActivity.kt    # PIN-protected settings
      │  ├─ SlotEditActivity.kt    # configure one button
      │  ├─ AppPickerActivity.kt   # choose any installed app
      │  ├─ ConfigStore.kt         # saved config + PIN
      │  ├─ PinManager.kt          # PIN dialogs
      │  ├─ AppLauncher.kt         # launching + icon resolution
      │  └─ Model.kt               # button model, icons, presets
      └─ res/                      # layouts, colourful icons, colours, theme
```

## Notes / things you might want to tweak

- **Default buttons** (first install): Phone, WhatsApp, Camera, Photos,
  Contacts, and one empty slot — change them in `Model.kt` (`Presets.defaultButtons`)
  or just reconfigure on the device.
- **Colours**: `app/src/main/res/values/colors.xml`.
- **Button count default** is 6; switch to 8 in settings.
- WhatsApp's button launches the `com.whatsapp` package; if she uses WhatsApp
  Business instead, edit it to point at that app via "Choose an installed app".
