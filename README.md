# Remind Me

A clean, local-first reminder app for Android that adapts to how you actually schedule things — not just a glorified alarm clock.

## Why another reminder app?

Most reminder apps force you into a single mental model: pick a date, pick a time, done. But reminders don't all work that way. "Drink water" needs to repeat every hour during the day. "Take vitamin D" is just a daily time. "Yoga" only happens on certain weekdays. "Mom's birthday" is a one-off.

Remind Me treats these as four distinct reminder types, each with a configuration screen tailored to its rhythm.

## Features

- **Four reminder types** — Interval (every X minutes within a time range), Once (specific date + time), Daily (one time, every day), Weekly (pick weekdays + time)
- **Quick toggle on/off per reminder** — pause without deleting
- **Color-coded categories** — Health, Work, Personal, or custom
- **Smart notifications** — full-screen alarm screen with Done / Snooze / Skip actions, looping alarm sound + vibration, bypasses Do Not Disturb
- **Streaks & stats** — 12-week heatmap and completion tracking to build habits
- **Templates** — one-tap presets for common reminders (water, meds, walks, sleep)
- **Search & filter** — find any reminder fast as your list grows
- **Quiet hours** — Do Not Disturb window so reminders don't wake you up
- **Dark mode** and customizable accent colors
- **Local-first** — no account needed, no data leaves your device

## Tech stack

- **Language:** Kotlin (JVM 17)
- **UI:** Jetpack Compose with Material 3
- **Local storage:** JSON file via Gson (`FileReminderRepository`); Room migration planned
- **Scheduling:** `AlarmManager` with exact alarms, re-armed per fire; a short-lived `shortService` foreground service hands off to the full-screen alarm activity so the takeover works on locked **and** unlocked devices (bypasses Android 12+ background-activity-launch restrictions)
- **Architecture:** Single Activity + Compose navigation, MVVM with a `ViewModel` per screen, manual DI via `AppContainer` (no Hilt), `sealed interface Reminder` so adding a new reminder type lights up every site the compiler needs you to handle

`minSdk 33`, `targetSdk 36`, `compileSdk 36`.

## Build & run

```bash
./gradlew assembleDebug      # build the debug APK
./gradlew installDebug       # install on a connected device / emulator
```

Open in Android Studio (Iguana or newer) for normal Run / Debug.

## Status

![Status](https://img.shields.io/badge/status-active%20development-orange)
![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/kotlin-2.1.0-7F52FF?logo=kotlin&logoColor=white)
![minSdk](https://img.shields.io/badge/minSdk-33-blue)
![AGP](https://img.shields.io/badge/AGP-9.1-success)

🚧 **In active development.** Core scheduling, notifications, and the full-screen alarm flow are working. UI polish, streak tracking, and the Room migration are in progress — expect breaking changes.

## License

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

```
MIT License

Copyright (c) 2026 Deepak Gupta

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
