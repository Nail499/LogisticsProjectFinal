package az.fleetra.mobile.network

import az.fleetra.mobile.network.dto.LiveTripResponse
import az.fleetra.mobile.network.dto.LocationRequest
import az.fleetra.mobile.network.dto.RatingDetailResponse
import az.fleetra.mobile.network.dto.RatingSummary
import az.fleetra.mobile.network.dto.Trip
import az.fleetra.mobile.network.dto.TripRejectRequest
import az.fleetra.mobile.network.dto.TripStatusUpdateRequest
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface DriverApi {
    @GET("api/driver/trips/current/live")
    suspend fun currentTripsLive(): List<LiveTripResponse>

    // Sürücüyə göndərilib hələ qəbul/imtina edilməmiş reyslər — bax
    // DriverController#pendingAcceptanceTrips. currentTripsLive-dan
    // AYRIDIR (PENDING_ACCEPTANCE orada YOXDUR).
    @GET("api/driver/trips/pending-acceptance")
    suspend fun pendingAcceptanceTrips(): List<LiveTripResponse>

    @POST("api/driver/trips/{id}/accept")
    suspend fun acceptTrip(@Path("id") tripId: Long): Trip

    @POST("api/driver/trips/{id}/reject")
    suspend fun rejectTrip(@Path("id") tripId: Long, @Body request: TripRejectRequest): Trip

    // DİQQƏT: backend əvvəllər xam List<Trip> qaytarırdı, indi
    // currentTripsLive ilə eyni zənginləşdirilmiş LiveTripResponse formasını
    // qaytarır (bax DriverController#tripHistory-dəki şərh) — List<Trip>
    // YANLIŞDIR, sahələr uyğun gəlmir (tripId vs id və s.).
    @GET("api/driver/trips/history")
    suspend fun tripHistory(): List<LiveTripResponse>

    @POST("api/driver/trips/{id}/status")
    suspend fun updateStatus(@Path("id") tripId: Long, @Body request: TripStatusUpdateRequest): Trip

    @GET("api/driver/ratings")
    suspend fun myRatings(): List<RatingDetailResponse>

    @GET("api/driver/ratings/summary")
    suspend fun myRatingSummary(): RatingSummary

    @POST("api/driver/trips/{id}/tracking")
    suspend fun sendLocation(@Path("id") tripId: Long, @Body request: LocationRequest)

    @Multipart
    @POST("api/driver/trips/{id}/proof")
    suspend fun uploadProof(@Path("id") tripId: Long, @Part photo: MultipartBody.Part): Trip
}
