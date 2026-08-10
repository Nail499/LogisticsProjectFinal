package az.fleetra.mobile.network

import az.fleetra.mobile.network.dto.ApiMessage
import az.fleetra.mobile.network.dto.ForgotPasswordRequest
import az.fleetra.mobile.network.dto.LoginRequest
import az.fleetra.mobile.network.dto.LoginResponse
import az.fleetra.mobile.network.dto.RegisterCustomerRequest
import az.fleetra.mobile.network.dto.RegisterCustomerResponse
import az.fleetra.mobile.network.dto.ResendCodeRequest
import az.fleetra.mobile.network.dto.ResetPasswordRequest
import az.fleetra.mobile.network.dto.VerifyCodeRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("api/auth/register/customer")
    suspend fun registerCustomer(@Body request: RegisterCustomerRequest): RegisterCustomerResponse

    // Uğurlu olduqda token qaytarır (avtomatik giriş) — bax AuthController#verifyEmail.
    @POST("api/auth/verify-email")
    suspend fun verifyEmail(@Body request: VerifyCodeRequest): LoginResponse

    @POST("api/auth/resend-verification")
    suspend fun resendVerification(@Body request: ResendCodeRequest): ApiMessage

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): ApiMessage

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): ApiMessage
}
