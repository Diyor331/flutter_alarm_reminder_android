package com.example.alarm_reminder

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class AlarmReminderPlugin : FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {
    private lateinit var channel: MethodChannel
    private lateinit var applicationContext: android.content.Context
    private var activity: Activity? = null
    private var activityBinding: ActivityPluginBinding? = null
    private var permissionResult: MethodChannel.Result? = null

    private val permissionListener =
        PluginRegistry.RequestPermissionsResultListener { requestCode, _, grantResults ->
            if (requestCode != requestNotificationPermissionCode) {
                return@RequestPermissionsResultListener false
            }

            val granted = grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            permissionResult?.success(granted)
            permissionResult = null
            true
        }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        applicationContext = binding.applicationContext
        channel = MethodChannel(binding.binaryMessenger, "alarm_reminder")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        try {
            when (call.method) {
                "getStatus" -> result.success(AlarmScheduler.getStatus(applicationContext))
                "requestNotificationPermission" -> requestNotificationPermission(result)
                "openExactAlarmSettings" -> {
                    AlarmScheduler.openExactAlarmSettings(applicationContext)
                    result.success(null)
                }
                "scheduleAlarm" -> {
                    val arguments = call.arguments as? Map<*, *>
                        ?: throw IllegalArgumentException("scheduleAlarm requires a map")
                    val payload = AlarmPayload.fromMap(arguments)
                    AlarmScheduler.scheduleAlarm(applicationContext, payload)
                    result.success(null)
                }
                "cancelAlarm" -> {
                    val id = (call.argument<Number>("id"))?.toInt()
                        ?: throw IllegalArgumentException("cancelAlarm requires id")
                    AlarmScheduler.cancelAlarm(applicationContext, id)
                    result.success(null)
                }
                "cancelAllAlarms" -> {
                    AlarmScheduler.cancelAll(applicationContext)
                    result.success(null)
                }
                else -> result.notImplemented()
            }
        } catch (error: IllegalStateException) {
            result.error("alarm_state_error", error.message, null)
        } catch (error: IllegalArgumentException) {
            result.error("alarm_argument_error", error.message, null)
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        activityBinding = binding
        binding.addRequestPermissionsResultListener(permissionListener)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        detachActivity()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivity() {
        detachActivity()
    }

    private fun requestNotificationPermission(result: MethodChannel.Result) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            result.success(true)
            return
        }

        val currentActivity = activity
        if (currentActivity == null) {
            result.error("activity_unavailable", "Notification permission requires a foreground activity", null)
            return
        }

        val granted = ContextCompat.checkSelfPermission(
            currentActivity,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            result.success(true)
            return
        }

        if (permissionResult != null) {
            result.error("permission_in_progress", "Notification permission request already running", null)
            return
        }

        permissionResult = result
        ActivityCompat.requestPermissions(
            currentActivity,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            requestNotificationPermissionCode,
        )
    }

    private fun detachActivity() {
        activityBinding?.removeRequestPermissionsResultListener(permissionListener)
        activityBinding = null
        activity = null
    }

    companion object {
        private const val requestNotificationPermissionCode = 1001
    }
}
