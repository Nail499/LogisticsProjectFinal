package com.ltc.logisticsproject.dto;

import com.ltc.logisticsproject.dto.customs.BorderCrossingView;
import com.ltc.logisticsproject.dto.customs.CustomsDeclarationView;
import com.ltc.logisticsproject.dto.customs.TradeDocumentView;
import com.ltc.logisticsproject.entity.CargoStatus;
import com.ltc.logisticsproject.entity.Incoterm;
import com.ltc.logisticsproject.entity.TransportMode;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

// Extended for the Stage 3 "Live Tracking" 30/70 dashboard (Fleetra):
// now also carries pickup/destination coordinates, the assigned driver's
// public-safe profile (name/phone/plate), and a naive straight-line ETA so
// the frontend can render the route polyline, driver card and countdown
// without any extra endpoints. Still fully public/unauthenticated.
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PublicTrackingResponse {
    String trackingNumber;
    CargoStatus status;
    String description;

    // Addım-addım mərhələ zolağı üçün (frontend: LiveTrackingPanel) — sifariş
    // nə vaxt qeydə alınıb, sürücü nə vaxt götürüb, nə vaxt çatdırıb.
    // "Yolda" mərhələsinin ayrıca vaxt möhürü yoxdur (Trip-də ayrıca sütun
    // saxlanmır) — frontend onu PICKED_UP ilə DELIVERED arasındakı "davam
    // edən" mərhələ kimi göstərir.
    String orderCreatedAt;
    String tripDeliveredAt;

    String pickupAddress;
    Double pickupLatitude;
    Double pickupLongitude;

    String destinationAddress;
    Double destinationLatitude;
    Double destinationLongitude;

    Double lastLatitude;
    Double lastLongitude;
    String lastUpdatedAt;

    String driverName;
    String driverPhone;
    String driverPhotoUrl;
    String vehiclePlate;
    // Nəqliyyat vasitəsinin sürücü tərəfindən yüklənmiş şəkilləri — 1 əsas +
    // ən çoxu 4 ətraflı (bax DriverController#uploadVehicleMainPhoto/
    // uploadVehicleDetailPhoto). Frontend bunları PhotoLightbox-da göstərir.
    String vehicleMainPhotoUrl;
    List<String> vehicleDetailPhotoUrls;

    Integer estimatedEtaMinutes;
    String tripStartedAt;

    // Çatdırılma sübutu (POD) — sürücü DELIVERED işarələməzdən əvvəl yüklədiyi
    // məcburi foto (bax DriverController#uploadProof, Trip.proofOfDeliveryUrl).
    String proofOfDeliveryUrl;

    // Yol boyu xərclər (fuel/toll/food/...) — reys yaranıbsa müştəriyə də
    // görünür, şübhəli (anomaly) qeyd olunanlar TripExpenseView.isAnomaly
    // ilə işarələnir ki, frontend onları ayrıca vurğulaya bilsin.
    List<TripExpenseView> expenses;

    // Beynəlxalq göndəriş məlumatları — daxili sifarişlərdə hamısı boş/null
    // qalır, frontend bu halda müvafiq bölmələri ümumiyyətlə göstərmir.
    boolean requiresCustoms;
    TransportMode preferredTransportMode;
    Incoterm incoterm;
    String originCountry;
    String destinationCountry;
    String transitCountries;

    List<TradeDocumentView> documents;
    CustomsDeclarationView customsDeclaration;
    List<BorderCrossingView> borderCrossings;
}
