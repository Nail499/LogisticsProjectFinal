package az.fleetra.mobile.network.dto

// GET /api/driver/trips/current/live, /pending-acceptance VƏ /trips/history
// (üçü də eyni zənginləşdirilmiş formanı qaytarır — bax
// DriverController#buildLiveTripResponse; /trips/history əvvəllər xam
// Trip entity qaytarırdı, indi bu deyil).
data class LiveTripResponse(
    val tripId: Long,
    val status: String?,
    val driverName: String?,
    val vehiclePlate: String?,
    val lastLatitude: Double?,
    val lastLongitude: Double?,
    val lastUpdatedAt: String?,
    val pickupAddress: String?,
    val destinationAddress: String?,
    val destinationLatitude: Double?,
    val destinationLongitude: Double?,
    val routeInfo: String?,
    val estimatedDistanceKm: Double?,
    val estimatedCost: Double?,
    val startedAt: String?,
    val deliveredAt: String?,
)

// GET /api/driver/trips/history — plain Trip entity (Cargo list is
// @JsonIgnore-d off it server-side).
data class Trip(
    val id: Long,
    val status: String?,
    val startedAt: String?,
    val deliveredAt: String?,
    val createdAt: String?,
    val estimatedDistanceKm: Double?,
    val estimatedCost: Double?,
    val routeInfo: String?,
    val proofOfDeliveryUrl: String?,
)

data class TripStatusUpdateRequest(val status: String)

// POST /api/driver/trips/{id}/reject — reason is optional (backend accepts
// an absent body entirely, but sending {"reason": null} is equally valid).
data class TripRejectRequest(val reason: String? = null)

data class LocationRequest(val latitude: Double, val longitude: Double)

data class ExpenseRequest(val category: String, val amount: Double, val description: String?)

data class FatigueAlertRequest(val continuousDrivingHours: Double)
