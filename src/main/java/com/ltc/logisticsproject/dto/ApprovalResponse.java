package com.ltc.logisticsproject.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApprovalResponse {
    Long applicationId;
    Long driverId;
    Long vehicleId;
    String username;
    String temporaryPassword;
    String message;
}