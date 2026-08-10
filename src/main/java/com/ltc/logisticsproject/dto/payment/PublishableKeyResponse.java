package com.ltc.logisticsproject.dto.payment;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PublishableKeyResponse {
    String publishableKey;
}
