package az.fleetra.mobile.network

import az.fleetra.mobile.network.dto.ChatCargoSummary
import az.fleetra.mobile.network.dto.ChatMessageRequest
import az.fleetra.mobile.network.dto.ChatMessageResponse
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

// Bax backend ChatController (/api/chat/cargo/{cargoId}/messages) və
// ChatCargoController (/api/chat/cargo-list). REST-only — backend-də STOMP
// @MessageMapping YOXDUR (yalnız server->client push), ona görə mobil
// tərəfdə polling istifadə olunur (bax ui/common/ChatScreen.kt).
// `channel` sətir kimi ötürülür ("CUSTOMER_DRIVER" / "CUSTOMER_DISPATCHER"
// / "INTERNAL") — Spring enum-a tam ad uyğunluğu ilə map edir.
interface ChatApi {
    @GET("api/chat/cargo/{cargoId}/messages")
    suspend fun history(@Path("cargoId") cargoId: Long, @Query("channel") channel: String): List<ChatMessageResponse>

    @POST("api/chat/cargo/{cargoId}/messages")
    suspend fun sendMessage(
        @Path("cargoId") cargoId: Long,
        @Query("channel") channel: String,
        @Body request: ChatMessageRequest,
    ): ChatMessageResponse

    @Multipart
    @POST("api/chat/cargo/{cargoId}/messages/image")
    suspend fun sendImage(
        @Path("cargoId") cargoId: Long,
        @Query("channel") channel: String,
        @Part image: MultipartBody.Part,
    ): ChatMessageResponse

    @GET("api/chat/cargo-list")
    suspend fun cargoList(): List<ChatCargoSummary>
}
