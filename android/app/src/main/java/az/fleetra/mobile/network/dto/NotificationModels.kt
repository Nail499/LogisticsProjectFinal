package az.fleetra.mobile.network.dto

// Bax backend entity/NotificationType.java — Gson enum adları tam eyni
// olmalıdır (fərqli/naməlum ad varsa Gson xəta atır, "lenient" adapter
// qoşulmayıb).
enum class NotificationType {
    WELCOME,
    ORDER_CREATED,
    PICKED_UP,
    IN_TRANSIT,
    DELIVERED,
    PASSWORD_CHANGED,
    PAYMENT_RECEIVED,
    NEW_CHAT_MESSAGE,
    NEW_ORDER,
    ORDER_CANCELLED,
    GENERAL,
    TRIP_ASSIGNED,
    TRIP_REJECTED,
    TRIP_CANCELLED,
    INCIDENT_REPORTED,
    DVIR_DEFECT,
}

// GET /api/notifications — backend xam Notification entity-sini qaytarır
// (əlavə DTO layer yoxdur). createdAt LocalDateTime-dır, digər String
// sahələr kimi burada da String saxlanılır (parse edilmir).
data class Notification(
    val id: Long,
    val userId: Long?,
    val type: NotificationType?,
    val title: String?,
    val message: String?,
    val link: String?,
    val read: Boolean?,
    val createdAt: String?,
)

// GET /api/notifications/unread-count cavabı: {"count": <long>}
data class UnreadCountResponse(val count: Long)
