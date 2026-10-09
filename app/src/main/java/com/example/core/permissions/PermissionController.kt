package com.example.core.permissions

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object PermissionController {
    const val PERMISSION_RECORD_AUDIO = android.Manifest.permission.RECORD_AUDIO
    const val PERMISSION_CAMERA = android.Manifest.permission.CAMERA

    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun hasAudioPermission(context: Context): Boolean =
        isPermissionGranted(context, PERMISSION_RECORD_AUDIO)

    fun hasCameraPermission(context: Context): Boolean =
        isPermissionGranted(context, PERMISSION_CAMERA)
}
