package az.fleetra.mobile.network.dto

// Bax backend entity/ChatChannel.java — Gson enum adları tam eyni
// olmalıdır. CUSTOMER_DRIVER: müştəri↔sürücü, CUSTOMER_DISPATCHER:
// müştəri↔dispetçer (sürücünün girişi YOXDUR), INTERNAL: sürücü↔dispetçer
// (müştərinin girişi YOXDUR) — bax ChatService#requireAccess.
enum class ChatChannel {
    CUSTOMER_DRIVER,
    CUSTOMER_DISPATCHER,
    INTERNAL,
}

// POST /api/chat/cargo/{cargoId}/messages body — bax dto/ChatMessageRequest.java.
data class ChatMessageRequest(val message: String)

// GET/POST /api/chat/cargo/{cargoId}/messages(/image) cavabı — bax
// dto/ChatMessageResponse.java. createdAt String-dir (LocalDateTime.
// toString()), parse edilmir.
data class ChatMessageResponse(
    val id: Long,
    val cargoId: Long?,
    val senderUserId: Long?,
    val senderName: String?,
    val senderRole: String?,
    val message: String?,
    val imageUrl: String?,
    val createdAt: String?,
    val mine: Boolean,
)

// GET /api/chat/cargo-list — söhbət "inbox"u, hər sifariş üçün bir yazı
// (kanal deyil). Bax dto/ChatCargoSummary.java.
data class ChatCargoSummary(
    val cargoId: Long,
    val trackingNumber: String?,
    val description: String?,
    val status: String?,
    val customerName: String?,
    val driverName: String?,
    val hasDriver: Boolean,
)
