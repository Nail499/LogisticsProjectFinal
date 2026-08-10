package com.ltc.logisticsproject.dto.payment;

import com.ltc.logisticsproject.entity.PaymentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

// Admin "Ödənişlər/Fakturalar" siyahısı — Payment sətrini Cargo/Customer
// ilə birləşdirib göstərir (bax AdminPaymentController).
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminPaymentView {
    Long id;
    Long cargoId;
    String trackingNumber;
    String customerName;
    String customerEmail;
    Double amount;
    String currency;
    PaymentStatus status;

    // "STRIPE" (onlayn kart) və ya "OFFLINE_DISPATCHER" (dispetçerin nağd/
    // bank köçürməsi kimi əl ilə qeydə aldığı ödəniş — bax
    // PaymentService#recordOfflinePayment). Frontend bunu fərqli badge ilə göstərir.
    String method;
    String offlineNote;

    String createdAt;
    String paidAt;
}
