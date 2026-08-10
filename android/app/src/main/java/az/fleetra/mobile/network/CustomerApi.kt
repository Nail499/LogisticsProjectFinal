package az.fleetra.mobile.network

import az.fleetra.mobile.network.dto.Cargo
import az.fleetra.mobile.network.dto.CargoRequest
import az.fleetra.mobile.network.dto.RatingRequest
import az.fleetra.mobile.network.dto.RatingResponse
import az.fleetra.mobile.network.dto.Warehouse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CustomerApi {
    @POST("api/customer/cargo")
    suspend fun createCargo(@Body request: CargoRequest): Cargo

    @GET("api/customer/cargo")
    suspend fun myOrders(): List<Cargo>

    @GET("api/customer/cargo/warehouses")
    suspend fun warehouses(): List<Warehouse>

    // Reytinq hələ yoxdursa backend 204 No Content qaytarır — nullable
    // qayıtma tipi Retrofit-in coroutine adapterinə bunu null-a çevirməyə
    // imkan verir (bax CustomerRatingController#getExisting).
    @GET("api/customer/trips/{tripId}/rating")
    suspend fun getRating(@Path("tripId") tripId: Long): RatingResponse?

    @POST("api/customer/trips/{tripId}/rating")
    suspend fun submitRating(@Path("tripId") tripId: Long, @Body request: RatingRequest): RatingResponse
}
