import 'package:alarm_reminder/alarm_reminder.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const ExampleApp());
}

class ExampleApp extends StatelessWidget {
  const ExampleApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Alarm Reminder Demo',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFFD6A957),
          brightness: Brightness.dark,
        ),
        scaffoldBackgroundColor: const Color(0xFF11120D),
        useMaterial3: true,
      ),
      home: const AlarmReminderDemoScreen(),
    );
  }
}

class AlarmReminderDemoScreen extends StatefulWidget {
  const AlarmReminderDemoScreen({super.key});

  @override
  State<AlarmReminderDemoScreen> createState() =>
      _AlarmReminderDemoScreenState();
}

class _AlarmReminderDemoScreenState extends State<AlarmReminderDemoScreen> {
  final _idController = TextEditingController(text: '101');
  final _titleController = TextEditingController(text: 'Renew subscription');
  final _bodyController = TextEditingController(text: 'LeaderTask');
  final _dismissController = TextEditingController();

  AlarmReminderStatus? _status;
  bool _busy = false;
  int _minutesFromNow = 1;
  DateTime? _lastScheduledAt;

  @override
  void initState() {
    super.initState();
    _refreshStatus();
  }

  @override
  void dispose() {
    _idController.dispose();
    _titleController.dispose();
    _bodyController.dispose();
    _dismissController.dispose();
    super.dispose();
  }

  Future<void> _refreshStatus() async {
    await _runGuarded(() async {
      final status = await AlarmReminder.getStatus();
      if (!mounted) {
        return;
      }
      setState(() {
        _status = status;
      });
    }, showLoader: false);
  }

  Future<void> _requestPermission() async {
    await _runGuarded(() async {
      await AlarmReminder.requestNotificationPermission();
      await _refreshStatus();
    });
  }

  Future<void> _openExactAlarmSettings() async {
    await _runGuarded(() async {
      await AlarmReminder.openExactAlarmSettings();
    });
  }

  Future<void> _scheduleAlarm() async {
    final id = int.tryParse(_idController.text.trim());
    if (id == null) {
      _showMessage('Alarm ID must be a number.');
      return;
    }

    final status = _status;
    if (status == null) {
      await _refreshStatus();
    }

    final currentStatus = _status;
    if (currentStatus != null && !currentStatus.exactAlarmAllowed) {
      _showMessage(
        'Exact alarms are disabled for this app. Open settings and allow exact alarms first.',
      );
      return;
    }

    final triggerAt = DateTime.now().add(Duration(minutes: _minutesFromNow));
    final dismissLabel = _dismissController.text.trim();

    await _runGuarded(() async {
      await AlarmReminder.schedule(
        AlarmReminderRequest(
          id: id,
          triggerAt: triggerAt,
          title: _titleController.text.trim(),
          body: _bodyController.text.trim(),
          dismissLabel: dismissLabel.isEmpty ? null : dismissLabel,
        ),
      );

      if (!mounted) {
        return;
      }

      setState(() {
        _lastScheduledAt = triggerAt;
      });
      await _refreshStatus();
      _showMessage('Alarm scheduled for ${_formatDateTime(triggerAt)}.');
    });
  }

  Future<void> _cancelAlarm() async {
    final id = int.tryParse(_idController.text.trim());
    if (id == null) {
      _showMessage('Alarm ID must be a number.');
      return;
    }

    await _runGuarded(() async {
      await AlarmReminder.cancel(id);
      await _refreshStatus();
      _showMessage('Alarm $id cancelled.');
    });
  }

  Future<void> _cancelAll() async {
    await _runGuarded(() async {
      await AlarmReminder.cancelAll();
      await _refreshStatus();
      _showMessage('All alarms cancelled.');
    });
  }

  Future<void> _runGuarded(
    Future<void> Function() action, {
    bool showLoader = true,
  }) async {
    if (_busy) {
      return;
    }

    if (showLoader && mounted) {
      setState(() {
        _busy = true;
      });
    }

    try {
      await action();
    } on PlatformException catch (error) {
      _showMessage(_describePlatformException(error));
    } catch (error) {
      _showMessage('Unexpected error: $error');
    } finally {
      if (showLoader && mounted) {
        setState(() {
          _busy = false;
        });
      }
    }
  }

  void _showMessage(String message) {
    if (!mounted) {
      return;
    }
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
  }

  String _describePlatformException(PlatformException error) {
    switch (error.code) {
      case 'alarm_state_error':
        return error.message ??
            'Alarm scheduling failed because a required system setting is disabled.';
      case 'alarm_argument_error':
        return error.message ?? 'Alarm request contains invalid data.';
      case 'activity_unavailable':
        return 'This action requires the app to be open in the foreground.';
      case 'permission_in_progress':
        return 'A notification permission request is already in progress.';
      default:
        final message = error.message;
        return message == null || message.isEmpty
            ? 'Platform error: ${error.code}'
            : 'Platform error: $message';
    }
  }

  String _formatDateTime(DateTime value) {
    final localizations = MaterialLocalizations.of(context);
    final time = localizations.formatTimeOfDay(
      TimeOfDay.fromDateTime(value),
      alwaysUse24HourFormat: MediaQuery.of(context).alwaysUse24HourFormat,
    );
    final date = localizations.formatMediumDate(value);
    return '$date, $time';
  }

  @override
  Widget build(BuildContext context) {
    final status = _status;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Alarm Reminder Demo'),
        actions: [
          IconButton(
            onPressed: _refreshStatus,
            icon: const Icon(Icons.refresh),
            tooltip: 'Refresh status',
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          _StatusPanel(status: status, formatDateTime: _formatDateTime),
          const SizedBox(height: 20),
          _SectionCard(
            title: 'Test payload',
            child: Column(
              children: [
                TextField(
                  controller: _idController,
                  keyboardType: TextInputType.number,
                  decoration: const InputDecoration(
                    labelText: 'Alarm ID',
                    hintText: '101',
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: _titleController,
                  decoration: const InputDecoration(
                    labelText: 'Title on lock screen',
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: _bodyController,
                  decoration: const InputDecoration(
                    labelText: 'Top caption / subtitle',
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: _dismissController,
                  decoration: const InputDecoration(
                    labelText: 'Dismiss button text (optional)',
                    hintText: 'Leave empty to use device locale',
                  ),
                ),
                const SizedBox(height: 16),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: [1, 2, 5, 10]
                      .map(
                        (minutes) => ChoiceChip(
                          label: Text('+$minutes min'),
                          selected: _minutesFromNow == minutes,
                          onSelected: (_) {
                            setState(() {
                              _minutesFromNow = minutes;
                            });
                          },
                        ),
                      )
                      .toList(),
                ),
                if (_lastScheduledAt != null) ...[
                  const SizedBox(height: 16),
                  Align(
                    alignment: Alignment.centerLeft,
                    child: Text(
                      'Last scheduled for ${_formatDateTime(_lastScheduledAt!)}',
                      style: Theme.of(context).textTheme.bodyMedium,
                    ),
                  ),
                ],
              ],
            ),
          ),
          const SizedBox(height: 20),
          _SectionCard(
            title: 'Actions',
            child: Column(
              children: [
                _ActionButton(
                  label: 'Request notification permission',
                  onPressed: _requestPermission,
                ),
                const SizedBox(height: 12),
                _ActionButton(
                  label: 'Open exact alarm settings',
                  onPressed: _openExactAlarmSettings,
                ),
                const SizedBox(height: 12),
                _ActionButton(
                  label: 'Schedule alarm',
                  onPressed: _scheduleAlarm,
                  primary: true,
                ),
                const SizedBox(height: 12),
                _ActionButton(
                  label: 'Cancel alarm by ID',
                  onPressed: _cancelAlarm,
                ),
                const SizedBox(height: 12),
                _ActionButton(
                  label: 'Cancel all alarms',
                  onPressed: _cancelAll,
                ),
              ],
            ),
          ),
          if (_busy) ...[
            const SizedBox(height: 20),
            const Center(child: CircularProgressIndicator()),
          ],
        ],
      ),
    );
  }
}

class _StatusPanel extends StatelessWidget {
  const _StatusPanel({
    required this.status,
    required this.formatDateTime,
  });

  final AlarmReminderStatus? status;
  final String Function(DateTime value) formatDateTime;

  @override
  Widget build(BuildContext context) {
    return _SectionCard(
      title: 'Current status',
      child: Column(
        children: [
          _StatusRow(
            label: 'Notifications granted',
            value: status?.notificationsGranted == true ? 'Yes' : 'No',
          ),
          const SizedBox(height: 12),
          _StatusRow(
            label: 'Exact alarms allowed',
            value: status?.exactAlarmAllowed == true ? 'Yes' : 'No',
          ),
          const SizedBox(height: 12),
          _StatusRow(
            label: 'Pending alarms',
            value: '${status?.pendingAlarmCount ?? 0}',
          ),
          const SizedBox(height: 12),
          _StatusRow(
            label: 'Pending IDs',
            value: status == null || status!.pendingAlarmIds.isEmpty
                ? 'None'
                : status!.pendingAlarmIds.join(', '),
          ),
          const SizedBox(height: 12),
          _StatusRow(
            label: 'Next scheduled',
            value: status?.nextScheduledAt == null
                ? 'None'
                : formatDateTime(status!.nextScheduledAt!),
          ),
        ],
      ),
    );
  }
}

class _StatusRow extends StatelessWidget {
  const _StatusRow({
    required this.label,
    required this.value,
  });

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(
          child: Text(
            label,
            style: textTheme.bodyLarge?.copyWith(
              color: Colors.white.withValues(alpha: 0.78),
            ),
          ),
        ),
        const SizedBox(width: 16),
        Flexible(
          child: Text(
            value,
            textAlign: TextAlign.end,
            style: textTheme.bodyLarge?.copyWith(
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
      ],
    );
  }
}

class _SectionCard extends StatelessWidget {
  const _SectionCard({
    required this.title,
    required this.child,
  });

  final String title;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFF1A1C15),
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.18),
            blurRadius: 18,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
              color: colorScheme.primary,
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 16),
          child,
        ],
      ),
    );
  }
}

class _ActionButton extends StatelessWidget {
  const _ActionButton({
    required this.label,
    required this.onPressed,
    this.primary = false,
  });

  final String label;
  final VoidCallback onPressed;
  final bool primary;

  @override
  Widget build(BuildContext context) {
    final style = primary
        ? FilledButton.styleFrom(
            minimumSize: const Size.fromHeight(52),
          )
        : OutlinedButton.styleFrom(
            minimumSize: const Size.fromHeight(52),
          );

    final child = Text(label);
    return SizedBox(
      width: double.infinity,
      child: primary
          ? FilledButton(
              onPressed: onPressed,
              style: style,
              child: child,
            )
          : OutlinedButton(
              onPressed: onPressed,
              style: style,
              child: child,
            ),
    );
  }
}
