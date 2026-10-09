package com.example.core.engine

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.Settings
import com.example.core.model.CommandResult
import com.example.core.model.ErrorType
import com.example.core.model.JarvisIntent
import com.example.core.model.VolumeAction
import com.example.core.safety.EmergencyStop
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocalCommandEngineImpl(
    private val context: Context
) : LocalCommandEngine {

    private val cameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    }

    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    private val batteryManager by lazy {
        context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    }

    override suspend fun execute(intent: JarvisIntent): CommandResult {
        // ALWAYS check EmergencyStop immediately before every execution
        if (EmergencyStop.isActive()) {
            return CommandResult.Error(
                message = "Emergency stop is active. Command rejected.",
                errorType = ErrorType.EMERGENCY_STOPPED,
                recoverySuggestion = "Reset emergency stop before issuing new commands"
            )
        }

        return when (intent) {
            is JarvisIntent.Flashlight -> handleFlashlight(intent.enable)
            is JarvisIntent.VolumeControl -> handleVolume(intent.action, intent.levelPercent)
            is JarvisIntent.BatteryStatus -> handleBattery()
            is JarvisIntent.DateTimeQuery -> handleDateTime()
            is JarvisIntent.LaunchApp -> handleLaunchApp(intent.appQuery)
            is JarvisIntent.OpenSettings -> handleOpenSettings(intent.settingType)
            is JarvisIntent.StopAll -> {
                EmergencyStop.trigger("User requested stop")
                CommandResult.Success("Emergency stop engaged. All systems paused.")
            }
            else -> CommandResult.Error(
                message = "LocalCommandEngine cannot directly handle intent: ${intent::class.simpleName}",
                errorType = ErrorType.INVALID_INPUT
            )
        }
    }

    private fun handleFlashlight(enable: Boolean): CommandResult {
        val manager = cameraManager ?: return CommandResult.Error(
            message = "Camera service unavailable on this device",
            errorType = ErrorType.HARDWARE_UNAVAILABLE
        )

        return try {
            val cameraIdList = manager.cameraIdList
            val rearCameraId = cameraIdList.firstOrNull { id ->
                val chars = manager.getCameraCharacteristics(id)
                val flashAvailable = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                val lensFacing = chars.get(CameraCharacteristics.LENS_FACING)
                flashAvailable && lensFacing == CameraCharacteristics.LENS_FACING_BACK
            } ?: cameraIdList.firstOrNull { id ->
                manager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
            }

            if (rearCameraId == null) {
                CommandResult.Error(
                    message = "Flashlight hardware not available on this device",
                    errorType = ErrorType.HARDWARE_UNAVAILABLE
                )
            } else {
                manager.setTorchMode(rearCameraId, enable)
                val state = if (enable) "ON" else "OFF"
                CommandResult.Success(
                    message = "Flashlight turned $state",
                    details = mapOf("state" to state, "cameraId" to rearCameraId),
                    audioFeedback = "Flashlight is now $state."
                )
            }
        } catch (e: CameraAccessException) {
            CommandResult.Error(
                message = "Failed to access camera flash: ${e.message}",
                errorType = ErrorType.EXECUTION_FAILED
            )
        } catch (e: SecurityException) {
            CommandResult.Error(
                message = "Camera permission not granted for flashlight",
                errorType = ErrorType.PERMISSION_DENIED
            )
        } catch (e: Exception) {
            CommandResult.Error(
                message = "Unexpected error toggling flashlight: ${e.message}",
                errorType = ErrorType.EXECUTION_FAILED
            )
        }
    }

    private fun handleVolume(action: VolumeAction, levelPercent: Int?): CommandResult {
        val am = audioManager ?: return CommandResult.Error(
            message = "Audio service unavailable",
            errorType = ErrorType.HARDWARE_UNAVAILABLE
        )

        return try {
            val streamType = AudioManager.STREAM_MUSIC
            val maxVolume = am.getStreamMaxVolume(streamType)
            val minVolume = am.getStreamMinVolume(streamType)
            val currentVolume = am.getStreamVolume(streamType)

            when (action) {
                VolumeAction.UP -> {
                    am.adjustStreamVolume(streamType, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    val newVol = am.getStreamVolume(streamType)
                    CommandResult.Success("Volume increased to $newVol/$maxVolume", audioFeedback = "Volume raised.")
                }
                VolumeAction.DOWN -> {
                    am.adjustStreamVolume(streamType, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                    val newVol = am.getStreamVolume(streamType)
                    CommandResult.Success("Volume lowered to $newVol/$maxVolume", audioFeedback = "Volume lowered.")
                }
                VolumeAction.MUTE -> {
                    am.adjustStreamVolume(streamType, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
                    CommandResult.Success("Audio muted", audioFeedback = "Muted.")
                }
                VolumeAction.UNMUTE -> {
                    am.adjustStreamVolume(streamType, AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI)
                    CommandResult.Success("Audio unmuted", audioFeedback = "Unmuted.")
                }
                VolumeAction.SET_LEVEL -> {
                    val targetPercent = (levelPercent ?: 50).coerceIn(0, 100)
                    val targetVolume = (minVolume + (maxVolume - minVolume) * (targetPercent / 100.0)).toInt()
                    am.setStreamVolume(streamType, targetVolume, AudioManager.FLAG_SHOW_UI)
                    CommandResult.Success("Volume set to $targetPercent%", audioFeedback = "Volume set to $targetPercent percent.")
                }
            }
        } catch (e: Exception) {
            CommandResult.Error(
                message = "Failed to adjust volume: ${e.message}",
                errorType = ErrorType.EXECUTION_FAILED
            )
        }
    }

    private fun handleBattery(): CommandResult {
        val bm = batteryManager
        val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val status = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val stateText = if (isCharging) "charging" else "discharging"
        return if (level >= 0) {
            CommandResult.Success(
                message = "Battery is at $level% ($stateText)",
                details = mapOf("level" to "$level%", "state" to stateText),
                audioFeedback = "Battery is at $level percent and currently $stateText."
            )
        } else {
            CommandResult.Success("Battery information available via Android system settings.")
        }
    }

    private fun handleDateTime(): CommandResult {
        val now = Date()
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateStr = dateFormat.format(now)
        val timeStr = timeFormat.format(now)

        return CommandResult.Success(
            message = "Today is $dateStr. The current time is $timeStr.",
            details = mapOf("date" to dateStr, "time" to timeStr),
            audioFeedback = "It is $timeStr on $dateStr."
        )
    }

    private fun handleLaunchApp(query: String): CommandResult {
        val pm = context.packageManager
        val normalized = query.lowercase().trim()

        // 1. YouTube priority check
        if (normalized.contains("youtube")) {
            val ytIntent = pm.getLaunchIntentForPackage("com.google.android.youtube")
            if (ytIntent != null) {
                ytIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(ytIntent)
                return CommandResult.Success(
                    message = "Opening YouTube app",
                    audioFeedback = "Opening YouTube."
                )
            } else {
                // Fallback to web browser YouTube
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                return CommandResult.Success(
                    message = "YouTube app not installed. Opening YouTube in browser.",
                    audioFeedback = "Opening YouTube in browser."
                )
            }
        }

        // 2. Camera shortcut
        if (normalized.contains("camera")) {
            val cameraIntent = Intent("android.media.action.IMAGE_CAPTURE").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (cameraIntent.resolveActivity(pm) != null) {
                context.startActivity(cameraIntent)
                return CommandResult.Success("Opening Camera", audioFeedback = "Opening Camera.")
            }
        }

        // 3. Search launchable applications
        val launchIntent = findLaunchIntentForName(pm, normalized)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            return CommandResult.Success("Opening $query", audioFeedback = "Opening $query.")
        }

        return CommandResult.Error(
            message = "Could not find an installed application matching '$query'",
            errorType = ErrorType.APP_NOT_FOUND,
            recoverySuggestion = "Verify the app name or install it from the Play Store"
        )
    }

    private fun findLaunchIntentForName(pm: android.content.pm.PackageManager, appName: String): Intent? {
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = pm.queryIntentActivities(mainIntent, 0)
        for (info in apps) {
            val label = info.loadLabel(pm).toString().lowercase()
            if (label.contains(appName) || info.activityInfo.packageName.lowercase().contains(appName)) {
                return pm.getLaunchIntentForPackage(info.activityInfo.packageName)
            }
        }
        return null
    }

    private fun handleOpenSettings(settingType: String): CommandResult {
        val action = when {
            settingType.contains("wifi") -> Settings.ACTION_WIFI_SETTINGS
            settingType.contains("bluetooth") -> Settings.ACTION_BLUETOOTH_SETTINGS
            settingType.contains("display") -> Settings.ACTION_DISPLAY_SETTINGS
            settingType.contains("sound") || settingType.contains("audio") -> Settings.ACTION_SOUND_SETTINGS
            settingType.contains("battery") -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            settingType.contains("application") || settingType.contains("app") -> Settings.ACTION_APPLICATION_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }

        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            CommandResult.Success(
                message = "Opened Android $settingType settings",
                audioFeedback = "Opening settings."
            )
        } catch (e: Exception) {
            CommandResult.Error(
                message = "Failed to open settings: ${e.message}",
                errorType = ErrorType.EXECUTION_FAILED
            )
        }
    }
}
