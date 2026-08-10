package az.fleetra.mobile.network

import az.fleetra.mobile.network.dto.CredentialsUpdateRequest
import az.fleetra.mobile.network.dto.ProfileResponse
import az.fleetra.mobile.network.dto.ProfileUpdateRequest
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Part

interface ProfileApi {
    @GET("api/profile")
    suspend fun me(): ProfileResponse

    @PUT("api/profile")
    suspend fun update(@Body request: ProfileUpdateRequest): ProfileResponse

    @PUT("api/profile/credentials")
    suspend fun updateCredentials(@Body request: CredentialsUpdateRequest): ProfileResponse

    @Multipart
    @POST("api/profile/photo")
    suspend fun uploadPhoto(@Part photo: MultipartBody.Part): ProfileResponse
}
