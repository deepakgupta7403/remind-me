# Remind Me

A clean, local-first reminder app for Android with four scheduling types — interval, once, daily, and weekly.

> **Built entirely with [Claude AI](https://claude.ai)** — UI/UX design, screens, icon, and implementation guidance.

## Why this app exists

I tried a lot of reminder apps on the Play Store. Every one of them was missing something:

- Some require **creating an account** just to set a daily reminder.
- Others lock the useful features (custom sounds, repeating intervals, themes) behind a **subscription**.
- Most don't support **full-screen notifications** — the kind that actually grab your attention when the reminder fires, instead of a tiny banner you swipe away without thinking.
- A few do one thing well but miss the basics (no weekly day picker, no quiet hours, no streak tracking).
- And none of them combined **all** the features I wanted in a single, free, no-strings-attached app.

So I built my own. **No login. No subscription. No ads. No data leaves your device.**

## Features

- **Four reminder types**
  - **Interval** — fires every X minutes within an active time range (e.g. drink water every hour from 8 am to 10 pm)
  - **Once** — specific date and time, for birthdays, appointments, deadlines
  - **Daily** — one time, every day, with optional "skip on weekends"
  - **Weekly** — pick the weekdays + a time
- **Full-screen notifications** with Done / Snooze / Skip actions — not just a banner
- **Quick on/off toggle** per reminder — pause without deleting
- **Color-coded categories** — Health, Work, Personal, or custom
- **Templates** — one-tap presets for common reminders (water, meds, walks, sleep, plants)
- **Streaks & stats** — 12-week heatmap and completion tracking
- **Search & filter** as your list grows
- **Quiet hours** — Do Not Disturb window so reminders don't wake you up
- **Custom notification sounds** per reminder type
- **Dark mode** and customizable accent colors
- **Local-first** — all data stored on device, no account required, fully offline

## Built with Claude AI

The entire app — every screen design, the icon, the navigation flow, the feature set, and the code — was designed and developed in collaboration with [Claude AI](https://claude.ai) by Anthropic. The project started as a list of feature requirements and grew through iterative conversation: Claude designed the 16 screens, picked the color palette, suggested the templates, and helped with implementation decisions like data modeling, notification scheduling, and the four-type architecture.

If you're considering building a personal app and aren't sure where to start, this project is proof you don't need a team — a clear idea and a good AI collaborator can take you a long way.

## Tech stack

- **Language:** Kotlin (JVM 17)
- **UI:** Jetpack Compose with Material 3
- **Local storage:** Room (reminders, activity stats, templates) via `RemindMeDatabase`; small session/identity state (onboarding flag, profile, search history) stays in SharedPreferences. Pre-Room data is migrated into Room once on first launch
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

🚧 **In active development.** Core scheduling, notifications, the full-screen alarm flow, and Room-backed storage are working. UI polish and streak tracking are in progress — expect breaking changes.

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
