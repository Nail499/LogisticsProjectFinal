package az.fleetra.mobile.network.dto

// GET /api/tracking/{trackingNumber} — public, unauthenticated. Bax backend
// PublicTrackingResponse.java — orada bundan daha çox sahə var (customs/
// expenses/documents və s., Faza 3-ə aiddir), burada sifariş detalı ekranı
// üçün lazım olanlar əlavə olunub, qalanı hələlik ötürülür (Gson naməlum
// JSON sahələrini xəta vermədən keçir).
data class PublicTrackingResponse(
    val trackingNumber: String?,
    val status: String?,
    val description: String?,
    val orderCreatedAt: String?,
    val tripDeliveredAt: String?,
    val pickupAddress: String?,
    val pickupLatitude: Double?,
    val pickupLongitude: Double?,
    val destinationAddress: String?,
    val destinationLatitude: Double?,
    val destinationLongitude: Double?,
    val lastLatitude: Double?,
    val lastLongitude: Double?,
    val lastUpdatedAt: String?,
    val driverName: String?,
    val driverPhone: String?,
    val driverPhotoUrl: String?,
    val vehiclePlate: String?,
    val vehicleMainPhotoUrl: String?,
    val estimatedEtaMinutes: Int?,
    val tripStartedAt: String?,
    val proofOfDeliveryUrl: String?,
)
