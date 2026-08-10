package com.ltc.logisticsproject.dto.profile;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProfileUpdateRequest {
    String fullName;

    // Ignored server-side for ADMIN/DISPATCHER — see ProfileController.
    LocalDate dateOfBirth;
    String nationality;
    String location;
}
