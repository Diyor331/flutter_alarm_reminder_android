import 'package:alarm_reminder/alarm_reminder.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('AlarmReminderRequest serializes to method channel payload', () {
    final triggerAt = DateTime.fromMillisecondsSinceEpoch(1_700_000_000_000);
    final request = AlarmReminderRequest(
      id: 42,
      triggerAt: triggerAt,
      title: 'Take medicine',
      body: 'Open the reminder',
      dismissLabel: 'Done',
    );

    expect(request.toMap(), <String, Object?>{
      'id': 42,
      'triggerAtMillis': triggerAt.millisecondsSinceEpoch,
      'title': 'Take medicine',
      'body': 'Open the reminder',
      'dismissLabel': 'Done',
    });
  });

  test('AlarmReminderStatus parses platform map', () {
    final status = AlarmReminderStatus.fromMap(<String, dynamic>{
      'notificationsGranted': true,
      'exactAlarmAllowed': true,
      'pendingAlarmCount': 2,
      'pendingAlarmIds': <int>[7, 8],
      'nextScheduledAtMillis': 1_700_000_000_000,
    });

    expect(status.notificationsGranted, isTrue);
    expect(status.exactAlarmAllowed, isTrue);
    expect(status.pendingAlarmCount, 2);
    expect(status.pendingAlarmIds, <int>[7, 8]);
    expect(
      status.nextScheduledAt,
      DateTime.fromMillisecondsSinceEpoch(1_700_000_000_000),
    );
  });
}
