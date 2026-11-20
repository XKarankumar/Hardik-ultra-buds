package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.audio.UltraSoundEngine
import com.example.bluetooth.BluetoothEarbudsManager
import com.example.data.PreferencesRepository
import com.example.update.AppUpdateManager
import com.example.update.UpdateCheckWorker
import java.util.concurrent.TimeUnit

class HardikBudsApp : Application() {

    lateinit var preferencesRepository: PreferencesRepository
        private set
    lateinit var audioEngine: UltraSoundEngine
        private set
    lateinit var bluetoothManager: BluetoothEarbudsManager
        private set
    lateinit var updateManager: AppUpdateManager
        private set

    override fun onCreate() {
        super.onCreate()
        preferencesRepository = PreferencesRepository(this)
        audioEngine = UltraSoundEngine(this)
        bluetoothManager = BluetoothEarbudsManager(this)
        updateManager = AppUpdateManager(this, preferencesRepository)

        createNotificationChannel()
        schedulePeriodicUpdateChecks()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                UPDATE_CHANNEL_ID,
                getString(R.string.notification_channel_updates),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_channel_updates_desc)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    private fun schedulePeriodicUpdateChecks() {
        runCatching {
            if (!WorkManager.isInitialized()) {
                WorkManager.initialize(
                    this,
                    androidx.work.Configuration.Builder().build()
                )
            }
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "hardik_github_update_check",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        audioEngine.release()
        bluetoothManager.release()
    }

    companion object {
        const val UPDATE_CHANNEL_ID = "hardik_system_updates"
    }
}
