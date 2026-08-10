package com.ltc.logisticsproject.dto.auth;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminSetPasswordRequest {
    String newPassword; // admin sets this directly — no current-password check, admin authority is the check
}
