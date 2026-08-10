package com.ltc.logisticsproject.dto.payment;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Dispetçerin "zəngli sifariş" üçün əl ilə qeydə aldığı ödənişin qısa
// (məcburi olmayan) qeydi — bax DispatcherPaymentController#recordOffline,
// PaymentService#recordOfflinePayment.
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OfflinePaymentRequest {
    String note;
}
