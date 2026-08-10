package com.ltc.logisticsproject.dto.push;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PushUnsubscribeRequest {
    String endpoint;
}
