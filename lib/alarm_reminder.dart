import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

const MethodChannel _channel = MethodChannel('alarm_reminder');

class AlarmReminder {
  const AlarmReminder._();

  static Future<AlarmReminderStatus> getStatus() async {
    final map = await _channel.invokeMapMethod<dynamic, dynamic>('getStatus');
    return AlarmReminderStatus.fromMap(
      map?.map((key, value) => MapEntry(key.toString(), value)) ?? const {},
    );
  }

  static Future<bool> requestNotificationPermission() async {
    final granted =
        await _channel.invokeMethod<bool>('requestNotificationPermission');
    return granted ?? false;
  }

  static Future<void> openExactAlarmSettings() {
    return _channel.invokeMethod<void>('openExactAlarmSettings');
  }

  static Future<void> schedule(AlarmReminderRequest request) {
    return _channel.invokeMethod<void>('scheduleAlarm', request.toMap());
  }

  static Future<void> cancel(int id) {
    return _channel.invokeMethod<void>('cancelAlarm', <String, Object?>{
      'id': id,
    });
  }

  static Future<void> cancelAll() {
    return _channel.invokeMethod<void>('cancelAllAlarms');
  }
}

@immutable
class AlarmReminderRequest {
  const AlarmReminderRequest({
    required this.id,
    required this.triggerAt,
    required this.title,
    required this.body,
    this.dismissLabel = 'Dismiss',
  });

  final int id;
  final DateTime triggerAt;
  final String title;
  final String body;
  final String dismissLabel;

  Map<String, Object?> toMap() {
    return <String, Object?>{
      'id': id,
      'triggerAtMillis': triggerAt.millisecondsSinceEpoch,
      'title': title,
      'body': body,
      'dismissLabel': dismissLabel,
    };
  }
}

@immutable
class AlarmReminderStatus {
  const AlarmReminderStatus({
    required this.notificationsGranted,
    required this.exactAlarmAllowed,
    required this.pendingAlarmCount,
    required this.pendingAlarmIds,
    required this.nextScheduledAt,
  });

  factory AlarmReminderStatus.fromMap(Map<String, dynamic> map) {
    final rawIds = (map['pendingAlarmIds'] as List<dynamic>? ?? const [])
        .map((value) => value as int)
        .toList(growable: false);

    return AlarmReminderStatus(
      notificationsGranted: map['notificationsGranted'] == true,
      exactAlarmAllowed: map['exactAlarmAllowed'] == true,
      pendingAlarmCount: (map['pendingAlarmCount'] as num?)?.toInt() ?? 0,
      pendingAlarmIds: rawIds,
      nextScheduledAt: _parseDateTime(map['nextScheduledAtMillis']),
    );
  }

  final bool notificationsGranted;
  final bool exactAlarmAllowed;
  final int pendingAlarmCount;
  final List<int> pendingAlarmIds;
  final DateTime? nextScheduledAt;

  static DateTime? _parseDateTime(Object? value) {
    final millis = (value as num?)?.toInt();
    if (millis == null || millis <= 0) {
      return null;
    }
    return DateTime.fromMillisecondsSinceEpoch(millis);
  }
}
