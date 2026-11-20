package com.example.update

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.HardikBudsApp
import com.example.MainActivity
import com.example.R
import com.example.model.UpdateStatus
import kotlinx.coroutines.flow.first

class UpdateCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? HardikBudsApp ?: return Result.success()
        val prefs = app.preferencesRepository.preferencesFlow.first()
        if (!prefs.autoUpdateCheckEnabled) return Result.success()

        val result = app.updateManager.checkForUpdates(prefs.githubRepoUrl, recordHistory = false)
        if (result is UpdateStatus.Available) {
            postUpdateNotification(result.latestVersion, result.releaseTitle)
        }
        return Result.success()
    }

    private fun postUpdateNotification(version: String, title: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val openIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_route", "update")
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            1001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            applicationContext,
            HardikBudsApp.UPDATE_CHANNEL_ID
        )
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("HARDIK SYSTEM UPDATE $version")
            .setContentText("$title is available on GitHub Releases.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(4201, notification)
    }
}
