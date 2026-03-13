import 'package:alarm_reminder/alarm_reminder.dart';
import 'package:flutter/material.dart';

void main() {
  runApp(const ExampleApp());
}

class ExampleApp extends StatefulWidget {
  const ExampleApp({super.key});

  @override
  State<ExampleApp> createState() => _ExampleAppState();
}

class _ExampleAppState extends State<ExampleApp> {
  AlarmReminderStatus? _status;

  @override
  void initState() {
    super.initState();
    _loadStatus();
  }

  Future<void> _loadStatus() async {
    final status = await AlarmReminder.getStatus();
    if (!mounted) {
      return;
    }

    setState(() {
      _status = status;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('alarm_reminder example')),
        body: Center(
          child: Text(
            'Exact alarms: ${_status?.exactAlarmAllowed ?? false}\n'
            'Pending: ${_status?.pendingAlarmCount ?? 0}',
            textAlign: TextAlign.center,
          ),
        ),
      ),
    );
  }
}
