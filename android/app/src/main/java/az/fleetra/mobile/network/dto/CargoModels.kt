package az.fleetra.mobile.network.dto

// Matches backend CargoType / UrgencyLevel enums exactly (Gson maps enum
// constants by name by default, so these must match the Java enum names).
enum class CargoType { GENERAL, FRAGILE, REFRIGERATED, HAZARDOUS }
enum class UrgencyLevel { STANDARD, EXPRESS }
enum class CargoStatus { PENDING, ASSIGNED, IN_TRANSIT, DELIVERED }

data class CargoRequest(
    val description: String,
    val weight: Double?,
    val volume: Double?,
    val originWarehouseId: Long? = null,
    val pickupAddress: String,
    val pickupLatitude: Double?,
    val pickupLongitude: Double?,
    val destinationAddress: String,
    val destinationLatitude: Double? = null,
    val destinationLongitude: Double? = null,
    val cargoType: CargoType,
    val urgency: UrgencyLevel,
    val requestedPickupDate: String? = null,
)

// Response shape for a Cargo row — see CustomerCargoController#myOrders.
// Qeyd: backend burada xam Cargo entity-sini qaytarır (əlavə DTO layer
// yoxdur), çox daha ətraflı sahələr (originWarehouse/customer/trip nested
// obyektləri və s.) var — Gson bilmədiyi sahələri sadəcə ötürür, xəta
// vermir. Sifariş detalı ekranı üçün lazım olan minimum əlavələr
// (price/paid/createdAt/cancelReason) aşağıda əlavə olunub.
data class Cargo(
    val id: Long,
    val trackingNumber: String?,
    val description: String?,
    val weight: Double?,
    val volume: Double?,
    val pickupAddress: String?,
    val destinationAddress: String?,
    val cargoType: String?,
    val urgency: String?,
    val status: String?,
    val price: Double?,
    val paid: Boolean?,
    val createdAt: String?,
    val cancelReason: String?,
    // Backend Cargo.trip sahəsi @JsonIgnore deyil, ona görə JSON-da tam
    // Trip obyekti gəlir — reytinq üçün lazım olan tripId buradan alınır
    // (bax CustomerRatingController: /api/customer/trips/{tripId}/rating).
    // Yalnız lazım olan sahələr modelləşdirilib, qalanı Gson tərəfindən
    // ötürülür.
    val trip: TripRef? = null,
)

data class TripRef(
    val id: Long,
    val status: String?,
)

data class Warehouse(
    val id: Long,
    val name: String?,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
)
