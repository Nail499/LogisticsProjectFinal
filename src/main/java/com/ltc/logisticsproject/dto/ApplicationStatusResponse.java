package com.ltc.logisticsproject.dto;

import com.ltc.logisticsproject.entity.ApplicationStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApplicationStatusResponse {
    Long id;
    ApplicationStatus status;
    String rejectionReason;
    String message;
}