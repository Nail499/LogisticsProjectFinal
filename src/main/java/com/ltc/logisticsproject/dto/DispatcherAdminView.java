package com.ltc.logisticsproject.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

// Admin "Dispetçerlər" list row. Deliberately excludes User.password (the
// BCrypt hash) — returning the raw User entity from a GET endpoint would
// leak it straight into the JSON response.
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DispatcherAdminView {
    Long id;
    String fullName;
    String username;
    Boolean enabled;
}
