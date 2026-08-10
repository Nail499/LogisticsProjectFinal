package com.ltc.logisticsproject.dto.payment;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentIntentResponse {
    Long paymentId;
    String clientSecret;
    String publishableKey;
    Double amount;
    String currency;
}
