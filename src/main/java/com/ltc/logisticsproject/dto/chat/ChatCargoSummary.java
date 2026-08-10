package com.ltc.logisticsproject.dto.chat;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Mərkəzi "Yazışma" bölməsindəki (bax ChatCargoController, frontend
// ChatHub.jsx) sifariş seçim kartı — hansı sifarişlər haqqında hansı tərəflə
// (müştəri/sürücü/dispetçer) yazışmaq mümkün olduğunu göstərmək üçün kifayət
// qədər məlumat.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatCargoSummary {
    Long cargoId;
    String trackingNumber;
    String description;
    String status;
    String customerName;
    String driverName;
    boolean hasDriver;
}
