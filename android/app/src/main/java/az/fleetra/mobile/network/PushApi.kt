package az.fleetra.mobile.network

import az.fleetra.mobile.network.dto.ApiMessage
import az.fleetra.mobile.network.dto.FcmTokenRequest
import retrofit2.http.Body
import retrofit2.http.POST

// Bax backend PushSubscriptionController (/api/push/fcm-subscribe,
// /api/push/fcm-unsubscribe) — bütün rollar üçün ortaq, autentifikasiya
// tələb olunur (Authorization header ApiClient-in interceptor-u ilə
// avtomatik əlavə olunur).
interface PushApi {
    @POST("api/push/fcm-subscribe")
    suspend fun fcmSubscribe(@Body request: FcmTokenRequest): ApiMessage

    @POST("api/push/fcm-unsubscribe")
    suspend fun fcmUnsubscribe(@Body request: FcmTokenRequest): ApiMessage
}
