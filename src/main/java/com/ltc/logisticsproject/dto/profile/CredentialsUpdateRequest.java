package com.ltc.logisticsproject.dto.profile;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CredentialsUpdateRequest {
    String currentPassword; // always required — proves the request isn't a hijacked session
    String newUsername; // optional — leave null/blank to keep the current one
    String newPassword; // optional — leave null/blank to keep the current one
}
