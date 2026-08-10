package az.fleetra.mobile.network

import az.fleetra.mobile.network.dto.PublicTrackingResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface TrackingApi {
    @GET("api/tracking/{trackingNumber}")
    suspend fun track(@Path("trackingNumber") trackingNumber: String): PublicTrackingResponse
}
