package com.ltc.logisticsproject.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Dispetçerin "Gözləyən yüklər" siyahısından bir yükü imtina edərkən
// göndərdiyi (məcburi olmayan) səbəb mətni — bax
// DispatcherController#rejectCargo.
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CargoRejectRequest {
    String reason;
}
