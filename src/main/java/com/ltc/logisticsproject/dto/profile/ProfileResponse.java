package com.ltc.logisticsproject.dto.profile;

import com.ltc.logisticsproject.entity.Role;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

// Stage 9 — self-service profile settings, shared by all 4 roles. Fields
// below role-fullName/username/password are only ever populated for
// CUSTOMER and DRIVER (see ProfileController) and stay null for
// ADMIN/DISPATCHER, whose accounts don't carry that extra data.
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProfileResponse {
    String username;
    String fullName;
    Role role;

    String photoUrl;
    LocalDate dateOfBirth;
    String nationality;
    String location;

    // CUSTOMER/DRIVER/DISPATCHER üçün doldurulur, ADMIN üçün null qalır
    // (bax ProfileController#toResponse) — profil səhifəsində Email bölməsi
    // yalnız bu sahə null olmayanda göstərilir.
    String email;
}
