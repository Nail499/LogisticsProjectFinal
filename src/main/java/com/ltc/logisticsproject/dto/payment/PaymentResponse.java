package com.ltc.logisticsproject.dto.payment;

import com.ltc.logisticsproject.entity.PaymentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentResponse {
    Long id;
    Long cargoId;
    Double amount;
    String currency;
    PaymentStatus status;
    String createdAt;
    String paidAt;
}
