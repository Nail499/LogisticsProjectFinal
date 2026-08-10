package az.fleetra.mobile.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import az.fleetra.mobile.MainActivity
import az.fleetra.mobile.network.ApiClient
import az.fleetra.mobile.network.dto.LocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest as FusedLocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// Stage — foreground GPS tracking while a driver has an active trip. Mirrors
// the Expo mobile app's expo-task-manager background location task, just
// implemented as a plain Android foreground Service (no extra framework).
class LocationTrackingService : Service() {

    companion object {
        private const val CHANNEL_ID = "fleetra_location_channel"
        private const val NOTIFICATION_ID = 4201
        const val EXTRA_TRIP_ID = "trip_id"
        const val ACTION_START = "az.fleetra.mobile.action.START_TRACKING"
        const val ACTION_STOP = "az.fleetra.mobile.action.STOP_TRACKING"

        fun start(context: Context, tripId: Long) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TRIP_ID, tripId)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, LocationTrackingService::class.java).apply { action = ACTION_STOP })
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedClient: FusedLocationProviderClient
    private var currentTripId: Long? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val tripId = currentTripId ?: return
            val location = result.lastLocation ?: return
            serviceScope.launch {
                try {
                    ApiClient.driverApi.sendLocation(tripId, LocationRequest(location.latitude, location.longitude))
                } catch (_: Exception) {
                    // Network hiccups are expected on the road — the next
                    // periodic tick will retry, no need to surface this.
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopTracking()
                return START_NOT_STICKY
            }
            else -> {
                val tripId = intent?.getLongExtra(EXTRA_TRIP_ID, -1L) ?: -1L
                if (tripId <= 0) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                currentTripId = tripId
                startForegroundNotification()
                startLocationUpdates()
            }
        }
        return START_STICKY
    }

    private fun startForegroundNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Fleetra — reys izlənilir")
            .setContentText("Canlı məkan məlumatı göndərilir")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Fleetra GPS izləmə", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startLocationUpdates() {
        val request = FusedLocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 20_000L)
            .setMinUpdateIntervalMillis(15_000L)
            .build()
        try {
            fusedClient.requestLocationUpdates(request, locationCallback, mainLooper)
        } catch (_: SecurityException) {
            // Location permission was revoked between the toggle and this
            // call — stop cleanly instead of crashing.
            stopSelf()
        }
    }

    private fun stopTracking() {
        fusedClient.removeLocationUpdates(locationCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        fusedClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
