# alarm_reminder

Flutter package plugin for Android full-screen alarm reminders.

## Package status

This repository is structured as a reusable Flutter plugin package:

- Dart API in `lib/alarm_reminder.dart`
- Android implementation in `android/src/main/...`
- Demo host app in `example/`

Use `example/` to run and verify the package locally. Do not use the repository
root as an application.

## What it does

- Schedules exact alarms with `AlarmManager`
- Opens a native full-screen alarm activity over the lock screen
- Plays ringtone and vibration when the reminder fires
- Restores scheduled reminders after reboot or app update
- Exposes a small Dart API for the host application

## Install from git

```yaml
dependencies:
  alarm_reminder:
    git:
      url: git@github.com:YOUR_ACCOUNT/alarm_reminder.git
```

For local development, you can also use a path dependency:

```yaml
dependencies:
  alarm_reminder:
    path: ../alarm_reminder
```

## Dart API

```dart
import 'package:alarm_reminder/alarm_reminder.dart';

await AlarmReminder.schedule(
  AlarmReminderRequest(
    id: 42,
    triggerAt: DateTime.now().add(const Duration(minutes: 10)),
    title: 'Take medicine',
    body: 'Open the full-screen reminder now.',
  ),
);
```

Available methods:

- `AlarmReminder.getStatus()`
- `AlarmReminder.requestNotificationPermission()`
- `AlarmReminder.openExactAlarmSettings()`
- `AlarmReminder.schedule(request)`
- `AlarmReminder.cancel(id)`
- `AlarmReminder.cancelAll()`

## Android notes

On Android 12 and above, the user must allow exact alarms in system settings.
On Android 13 and above, the app must also have notification permission.

## Scope

This package is intentionally focused on alarm-style reminders only. It does
not replace regular notifications such as `firebase_messaging` or
`flutter_local_notifications`.
