package az.fleetra.mobile.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import az.fleetra.mobile.MainActivity
import az.fleetra.mobile.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

// Bax AndroidManifest.xml-dəki <service> qeydi (com.google.firebase.
// MESSAGING_EVENT intent-filter). Firebase qoşulmayıbsa bu servis heç vaxt
// instansiyalaşdırılmır — google-services.json olmadan real push mesajı
// gəlmir.
class FleetraFcmService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "fleetra_push_channel"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FcmTokenRegistrar.subscribe(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "Fleetra"
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        ensureChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            // POST_NOTIFICATIONS icazəsi manifestdə bəyan edilib, lakin
            // Android 13+ üzərində runtime təsdiqi əlavə axın tələb edir
            // (hələ tətbiq edilməyib — bax README-dəki Faza 1 son yoxlama
            // qeydi). İcazə verilməyibsə SecurityException atıla bilər,
            // sakitcə tutulur ki, tətbiq çökməsin.
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
        } catch (_: SecurityException) {
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Fleetra bildirişləri",
                NotificationManager.IMPORTANCE_HIGH,
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
}
