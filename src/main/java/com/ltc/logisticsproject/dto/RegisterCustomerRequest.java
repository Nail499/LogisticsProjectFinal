package com.ltc.logisticsproject.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterCustomerRequest {
    String username;
    String password;
    String fullName;
    String phone;
    String email;
    String companyName;
}