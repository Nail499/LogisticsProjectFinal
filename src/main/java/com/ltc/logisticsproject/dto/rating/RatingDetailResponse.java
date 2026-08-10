package com.ltc.logisticsproject.dto.rating;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Admin/dispetçer/sürücü panellərində "hansı reysdən nə qiymət/şərh alınıb"
// sualına cavab verən ətraflı görünüş (bax RatingService#toDetail). Sadə
// RatingResponse (müştəri özününü görəndə) fərqli olaraq bura sürücü/müştəri
// adı, tracking № və marşrut da əlavə olunur ki, ayrıca sorğu getmədən
// siyahıda birbaşa "kim kimə, hansı reysə görə" oxuna bilsin.
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RatingDetailResponse {
    Long id;
    Long tripId;
    Long driverId;
    String driverName;
    String vehiclePlate;
    String customerName;
    String trackingNumber;
    String routeInfo;
    Integer stars;
    String comment;
    String createdAt;
    String deliveredAt;
}
