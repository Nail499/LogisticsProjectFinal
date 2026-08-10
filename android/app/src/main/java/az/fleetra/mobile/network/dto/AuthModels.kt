package az.fleetra.mobile.network.dto

data class LoginRequest(val username: String, val password: String)

// Mirrors backend AuthController#login -> LoginResponse(token, role, username)
data class LoginResponse(val token: String, val role: String, val username: String)

data class RegisterCustomerRequest(
    val fullName: String,
    val phone: String,
    val email: String?,
    val companyName: String?,
    val username: String,
    val password: String,
)

// POST /api/auth/register/customer cavabı — token yoxdur, sonra
// /verify-email ilə təsdiqləmə lazımdır (bax AuthController#registerCustomer).
data class RegisterCustomerResponse(val message: String?, val username: String?)

// POST /api/auth/verify-email — uğurlu olduqda LoginResponse qaytarılır
// (avtomatik giriş).
data class VerifyCodeRequest(val username: String, val code: String)

// POST /api/auth/resend-verification
data class ResendCodeRequest(val username: String)

// POST /api/auth/forgot-password
data class ForgotPasswordRequest(val email: String)

// POST /api/auth/reset-password
data class ResetPasswordRequest(val email: String, val code: String, val newPassword: String)
