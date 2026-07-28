package com.ltc.logisticsproject.dto;

import com.ltc.logisticsproject.entity.ApplicationStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApplicationStatusResponse {
    // Ardıcıl DB id-si YOX — ictimai/auth-suz status yoxlama üçün istifadə
    // olunan unikal kod (bax JobApplication.applicationCode). id qəsdən
    // client-ə ötürülmür ki, ardıcıl nömrələr sadalanaraq başqa müraciətlərin
    // statusu/rədd səbəbi "təxmin edilə" bilməsin.
    String applicationCode;
    ApplicationStatus status;
    String rejectionReason;
    String message;
}