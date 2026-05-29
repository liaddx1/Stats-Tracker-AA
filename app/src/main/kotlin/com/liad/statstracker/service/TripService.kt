package com.liad.statstracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
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
        if (!promoteToForeground()) {
            // Couldn't become a foreground service at all. Clear any foreground
            // state and stop cleanly so the startForegroundService() contract is
            // satisfied and the framework doesn't kill the process with
            // "did not then call Service.startForeground()".
            stopForeground(STOP_FOREGROUND_REMOVE)
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

    private fun promoteToForeground(): Boolean {
        val notif = buildNotification()
        // Preferred: the "location" type keeps GPS flowing while the phone screen
        // is off — but the system only allows starting it from the background (a
        // locked phone) when location access is valid there, i.e. the user chose
        // "Allow all the time". Otherwise startForeground throws
        // ForegroundServiceStartNotAllowedException / SecurityException on API 34+.
        if (tryStartForeground(notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)) return true
        // Fall back to a non-restricted type purely to satisfy the
        // startForegroundService() contract so the process stays alive instead of
        // crashing. Background GPS is unavailable in this mode (the Car UI shows
        // "no GPS") until the user grants background location.
        Log.w(TAG, "location FGS rejected; falling back to dataSync (no background GPS)")
        return tryStartForeground(notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    private fun tryStartForeground(notif: Notification, type: Int): Boolean = try {
        startForeground(NOTIFICATION_ID, notif, type)
        true
    } catch (e: Exception) {
        Log.w(TAG, "startForeground(type=$type) rejected", e)
        false
    }

    private fun buildNotification(): Notification {
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), flags,
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, TripService::class.java).setAction(ACTION_STOP),
            flags,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.tracking_notification_title))
            .setContentText(getString(R.string.tracking_notification_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.tracking_notification_stop), stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
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
