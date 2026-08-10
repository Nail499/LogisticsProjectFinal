package az.fleetra.mobile.network.dto

// GET/PUT /api/profile — see backend ProfileController. dateOfBirth stays a
// plain ISO "yyyy-MM-dd" string here since Gson has no built-in java.time
// support and the backend already serializes LocalDate that way.
data class ProfileResponse(
    val username: String,
    val fullName: String?,
    val role: String,
    val photoUrl: String?,
    val dateOfBirth: String?,
    val nationality: String?,
    val location: String?,
)

data class ProfileUpdateRequest(
    val fullName: String?,
    val dateOfBirth: String?,
    val nationality: String?,
    val location: String?,
)

data class CredentialsUpdateRequest(
    val currentPassword: String,
    val newUsername: String?,
    val newPassword: String?,
)

data class ApiMessage(val message: String?)
