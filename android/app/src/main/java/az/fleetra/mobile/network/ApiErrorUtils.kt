package az.fleetra.mobile.network

import az.fleetra.mobile.network.dto.ApiMessage
import retrofit2.HttpException

// Backend error bodies come in two shapes depending on the endpoint: some
// return a JSON object like {"message": "..."} (ProfileController,
// AdminManagementController), others return a bare JSON string (older
// controllers, e.g. AuthController's ResponseEntity.badRequest().body("...")).
// This tries the structured shape first and falls back to raw text.
fun extractErrorMessage(e: Throwable, fallback: String): String {
    if (e !is HttpException) return e.message ?: fallback
    val body = e.response()?.errorBody()?.string() ?: return fallback
    return try {
        ApiClient.gson.fromJson(body, ApiMessage::class.java)?.message ?: body.trim('"').ifBlank { fallback }
    } catch (_: Exception) {
        body.trim('"').ifBlank { fallback }
    }
}
