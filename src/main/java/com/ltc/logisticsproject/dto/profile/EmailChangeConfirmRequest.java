package com.ltc.logisticsproject.dto.profile;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Profildə "email dəyişdir" axınının 2-ci (təsdiq) addımı — bax
// ProfileController#confirmEmailChange.
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailChangeConfirmRequest {
    String code;
}
