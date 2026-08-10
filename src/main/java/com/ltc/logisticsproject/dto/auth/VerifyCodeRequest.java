package com.ltc.logisticsproject.dto.auth;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Qeydiyyat email-təsdiqi üçün: istifadəçi adı + email-ə gələn 6 rəqəmli kod.
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VerifyCodeRequest {
    String username;
    String code;
}
