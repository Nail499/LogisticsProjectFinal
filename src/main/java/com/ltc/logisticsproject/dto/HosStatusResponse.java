package com.ltc.logisticsproject.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Sürücü panelindəki RestModeCard üçün — bax HosService#buildStatus.
// status: "DRIVING" | "RESTING" | "NONE" (heç vaxt toggle edilməyib).
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HosStatusResponse {
    String status;
    String segmentStartedAt;
    long continuousDrivingSeconds;
    long todayDrivingSeconds;
    boolean fatigueWarning;
}
