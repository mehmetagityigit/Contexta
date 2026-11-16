package com.example.feature.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

object OverlayPermissionHelper {
    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun buildOverlayPermissionIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun startFloatingCapture(context: Context): Boolean {
        if (!canDrawOverlays(context)) return false
        val intent = Intent(context, FloatingCaptureService::class.java).apply {
            action = FloatingCaptureService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.startService(intent)
        }
        return true
    }

    fun stopFloatingCapture(context: Context) {
        val intent = Intent(context, FloatingCaptureService::class.java).apply {
            action = FloatingCaptureService.ACTION_STOP
        }
        context.startService(intent)
    }
}
