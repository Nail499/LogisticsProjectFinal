package az.fleetra.mobile.network.dto

// GET /api/driver/ratings — bax dto/RatingDetailResponse.java. Diqqət:
// createdAt/deliveredAt backend-də LocalDateTime yox, String-dir (belə
// serialize olunur), ona görə burada da String.
data class RatingDetailResponse(
    val id: Long,
    val tripId: Long?,
    val driverId: Long?,
    val driverName: String?,
    val vehiclePlate: String?,
    val customerName: String?,
    val trackingNumber: String?,
    val routeInfo: String?,
    val stars: Int?,
    val comment: String?,
    val createdAt: String?,
    val deliveredAt: String?,
)

// GET /api/driver/ratings/summary — bax RatingService.RatingSummary
// (record RatingSummary(double average, int count)).
data class RatingSummary(
    val average: Double,
    val count: Int,
)

// POST /api/customer/trips/{tripId}/rating — bax dto/RatingRequest.java.
data class RatingRequest(
    val stars: Int?,
    val comment: String?,
)

// GET/POST /api/customer/trips/{tripId}/rating cavabı — bax
// dto/RatingResponse.java. GET-də reytinq hələ yoxdursa backend 204 No
// Content qaytarır (bax CustomerApi#getRating-də nullable return type).
data class RatingResponse(
    val id: Long?,
    val tripId: Long?,
    val stars: Int?,
    val comment: String?,
    val createdAt: String?,
)
