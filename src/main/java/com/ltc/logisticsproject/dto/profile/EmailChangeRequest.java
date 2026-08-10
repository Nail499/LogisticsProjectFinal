package com.ltc.logisticsproject.dto.profile;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Profildə "email dəyişdir" axınının 1-ci addımı — bax
// ProfileController#requestEmailChange.
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailChangeRequest {
    String newEmail;
}
