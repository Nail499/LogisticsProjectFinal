package az.fleetra.mobile.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az.fleetra.mobile.config.ApiConfig
import az.fleetra.mobile.data.TokenStore
import az.fleetra.mobile.data.UserSession
import az.fleetra.mobile.network.ApiClient
import az.fleetra.mobile.network.dto.ForgotPasswordRequest
import az.fleetra.mobile.network.dto.LoginRequest
import az.fleetra.mobile.network.dto.RegisterCustomerRequest
import az.fleetra.mobile.network.dto.RegisterCustomerResponse
import az.fleetra.mobile.network.dto.ResendCodeRequest
import az.fleetra.mobile.network.dto.ResetPasswordRequest
import az.fleetra.mobile.network.dto.VerifyCodeRequest
import az.fleetra.mobile.network.extractErrorMessage
import az.fleetra.mobile.messaging.FcmTokenRegistrar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class AuthViewModel : ViewModel() {
    private val _session = MutableStateFlow<UserSession?>(null)
    val session: StateFlow<UserSession?> = _session.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        viewModelScope.launch {
            TokenStore.hydrate()
            val token = TokenStore.cachedToken
            val username = TokenStore.cachedUsername
            val role = TokenStore.cachedRole
            _session.value = if (token != null && username != null && role != null) {
                UserSession(token, username, role)
            } else null
            _loading.value = false
        }
    }

    suspend fun login(username: String, password: String): Result<UserSession> {
        return try {
            val res = ApiClient.authApi.login(LoginRequest(username, password))
            TokenStore.saveSession(res.token, res.username, res.role)
            val session = UserSession(res.token, res.username, res.role)
            _session.value = session
            FcmTokenRegistrar.registerCurrentToken()
            Result.success(session)
        } catch (e: HttpException) {
            val msg = if (e.code() == 401 || e.code() == 403) {
                "İstifadəçi adı və ya şifrə yanlışdır"
            } else {
                extractErrorMessage(e, "Server xətası (${e.code()})")
            }
            Result.failure(Exception(msg))
        } catch (e: IOException) {
            // No HTTP response at all — the request never reached the
            // server. Wrong IP in ApiConfig, backend not running, or
            // phone/PC not on the same network. NOT a credentials problem.
            Result.failure(Exception("Backend-ə qoşula bilmədi (${ApiConfig.BASE_URL}). IP ünvanını və backend-in işlək olduğunu yoxlayın."))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Naməlum xəta baş verdi"))
        }
    }

    // Qeydiyyat token qaytarmır — sonra kod ilə /verify-email çağırılmalıdır
    // (bax AuthController#registerCustomer, iki addımlı axın).
    suspend fun registerCustomer(request: RegisterCustomerRequest): Result<RegisterCustomerResponse> {
        return try {
            Result.success(ApiClient.authApi.registerCustomer(request))
        } catch (e: HttpException) {
            Result.failure(Exception(extractErrorMessage(e, "Qeydiyyat uğursuz oldu (${e.code()})")))
        } catch (e: IOException) {
            Result.failure(Exception("Backend-ə qoşula bilmədi (${ApiConfig.BASE_URL})."))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Naməlum xəta baş verdi"))
        }
    }

    // Uğurlu olduqda token qaytarır — avtomatik giriş edilir (login-lə eyni
    // məntiq, bax AuthController#verifyEmail-dəki şərh).
    suspend fun verifyEmail(username: String, code: String): Result<UserSession> {
        return try {
            val res = ApiClient.authApi.verifyEmail(VerifyCodeRequest(username, code))
            TokenStore.saveSession(res.token, res.username, res.role)
            val session = UserSession(res.token, res.username, res.role)
            _session.value = session
            FcmTokenRegistrar.registerCurrentToken()
            Result.success(session)
        } catch (e: HttpException) {
            Result.failure(Exception(extractErrorMessage(e, "Kod yanlışdır (${e.code()})")))
        } catch (e: IOException) {
            Result.failure(Exception("Backend-ə qoşula bilmədi (${ApiConfig.BASE_URL})."))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Naməlum xəta baş verdi"))
        }
    }

    suspend fun resendVerificationCode(username: String): Result<String> {
        return try {
            val res = ApiClient.authApi.resendVerification(ResendCodeRequest(username))
            Result.success(res.message ?: "Kod yenidən göndərildi")
        } catch (e: HttpException) {
            Result.failure(Exception(extractErrorMessage(e, "Kod göndərilmədi (${e.code()})")))
        } catch (e: IOException) {
            Result.failure(Exception("Backend-ə qoşula bilmədi (${ApiConfig.BASE_URL})."))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Naməlum xəta baş verdi"))
        }
    }

    suspend fun forgotPassword(email: String): Result<String> {
        return try {
            val res = ApiClient.authApi.forgotPassword(ForgotPasswordRequest(email))
            Result.success(res.message ?: "Kod göndərildi")
        } catch (e: HttpException) {
            Result.failure(Exception(extractErrorMessage(e, "Sorğu uğursuz oldu (${e.code()})")))
        } catch (e: IOException) {
            Result.failure(Exception("Backend-ə qoşula bilmədi (${ApiConfig.BASE_URL})."))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Naməlum xəta baş verdi"))
        }
    }

    suspend fun resetPassword(email: String, code: String, newPassword: String): Result<String> {
        return try {
            val res = ApiClient.authApi.resetPassword(ResetPasswordRequest(email, code, newPassword))
            Result.success(res.message ?: "Şifrəniz yeniləndi")
        } catch (e: HttpException) {
            Result.failure(Exception(extractErrorMessage(e, "Şifrə yenilənmədi (${e.code()})")))
        } catch (e: IOException) {
            Result.failure(Exception("Backend-ə qoşula bilmədi (${ApiConfig.BASE_URL})."))
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Naməlum xəta baş verdi"))
        }
    }

    fun logout() {
        FcmTokenRegistrar.unregisterCurrentToken()
        viewModelScope.launch {
            TokenStore.clearSession()
            _session.value = null
        }
    }
}
