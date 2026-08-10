package az.fleetra.mobile.messaging

import android.util.Log
import az.fleetra.mobile.data.TokenStore
import az.fleetra.mobile.network.ApiClient
import az.fleetra.mobile.network.dto.FcmTokenRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Backend-ə cari FCM tokenini göndərir/silir — bax
// PushSubscriptionController#registerFcmToken/#unregisterFcmToken.
// Firebase qoşulmayıbsa (google-services.json yoxdursa)
// FirebaseMessaging.getInstance() İstisna atır — bu sakitcə tutulur, push
// funksionallığı sadəcə passiv qalır, tətbiq normal işləyir (bax
// app/build.gradle.kts-dəki şərti google-services tətbiqi).
object FcmTokenRegistrar {

    // AuthViewModel#login/#verifyEmail uğurlu olduqda çağırılır.
    fun registerCurrentToken() {
        try {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token -> subscribe(token) }
        } catch (e: Exception) {
            Log.d("FcmTokenRegistrar", "Firebase qoşulmayıb, FCM qeydiyyatı keçildi: ${e.message}")
        }
    }

    // FirebaseMessagingService#onNewToken tərəfindən də çağırılır (token
    // yeniləndikdə).
    fun subscribe(token: String) {
        if (TokenStore.cachedToken == null) return // istifadəçi hələ giriş etməyib
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ApiClient.pushApi.fcmSubscribe(FcmTokenRequest(token))
            } catch (_: Exception) {
                // Qeydiyyat uğursuz olsa da tətbiq normal işləməyə davam edir.
            }
        }
    }

    // AuthViewModel#logout tərəfindən çağırılır.
    fun unregisterCurrentToken() {
        try {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        ApiClient.pushApi.fcmUnsubscribe(FcmTokenRequest(token))
                    } catch (_: Exception) {
                    }
                }
            }
        } catch (_: Exception) {
        }
    }
}
