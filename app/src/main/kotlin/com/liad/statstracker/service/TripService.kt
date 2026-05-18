package com.liad.statstracker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.liad.statstracker.R
import com.liad.statstracker.data.LocationProvider
import com.liad.statstracker.data.PerformanceRepository
import com.liad.statstracker.data.SensorRepository
import com.liad.statstracker.data.TripStatsRepository
import com.liad.statstracker.phone.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class TripService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var locationJob: Job? = null
    private var sensorJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        if (!startForegroundWithNotification()) {
            stopSelf()
            return
        }
        TripStatsRepository.get(this)
        PerformanceRepository.get(this)
        locationJob = scope.launch {
            LocationProvider.get(this@TripService).location.collect { }
        }
        sensorJob = scope.launch {
            SensorRepository.get(this@TripService).state.collect { }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        locationJob?.cancel()
        sensorJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun startForegroundWithNotification(): Boolean {
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), flags,
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, TripService::class.java).setAction(ACTION_STOP),
            flags,
        )
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.tracking_notification_title))
            .setContentText(getString(R.string.tracking_notification_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.tracking_notification_stop), stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(NOTIFICATION_ID, notif)
            }
            true
        } catch (e: Exception) {
            // Locked-phone start from Car App session on API 34+ can throw
            // ForegroundServiceStartNotAllowedException / SecurityException when
            // ACCESS_BACKGROUND_LOCATION isn't granted. Fail soft so the process
            // stays alive — the Car session UI will show "no GPS" instead of crashing.
            Log.w(TAG, "startForeground rejected; service will stop", e)
            false
        }
    }

    private fun ensureChannel() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.tracking_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.tracking_channel_description)
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "TripService"
        private const val CHANNEL_ID = "trip_tracking"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.liad.statstracker.action.STOP"

        fun start(context: Context) {
            val intent = Intent(context, TripService::class.java)
            try {
                context.startForegroundService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "startForegroundService failed", e)
                try {
                    context.startService(intent)
                } catch (e2: Exception) {
                    Log.w(TAG, "startService fallback failed", e2)
                }
            }
        }
    }
}
