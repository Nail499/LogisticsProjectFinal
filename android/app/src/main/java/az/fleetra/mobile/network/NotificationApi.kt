package az.fleetra.mobile.network

import az.fleetra.mobile.network.dto.ApiMessage
import az.fleetra.mobile.network.dto.Notification
import az.fleetra.mobile.network.dto.UnreadCountResponse
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

// Bütün rollar üçün ortaq — bax backend NotificationController
// (/api/notifications, .anyRequest().authenticated() altına düşür).
interface NotificationApi {
    @GET("api/notifications")
    suspend fun list(): List<Notification>

    @GET("api/notifications/unread-count")
    suspend fun unreadCount(): UnreadCountResponse

    @POST("api/notifications/{id}/read")
    suspend fun markAsRead(@Path("id") id: Long): Notification

    @POST("api/notifications/read-all")
    suspend fun markAllAsRead(): ApiMessage
}
