package com.ltc.logisticsproject.dto.push;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Mobil tətbiqin Firebase-dən aldığı qeydiyyat tokeni — bax
// PushSubscriptionController#registerFcmToken, entity/FcmToken.java.
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FcmTokenRequest {
    String token;
}
