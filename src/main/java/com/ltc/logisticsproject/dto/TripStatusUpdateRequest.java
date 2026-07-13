package com.ltc.logisticsproject.dto;

import com.ltc.logisticsproject.entity.TripStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TripStatusUpdateRequest {
    TripStatus status;
}