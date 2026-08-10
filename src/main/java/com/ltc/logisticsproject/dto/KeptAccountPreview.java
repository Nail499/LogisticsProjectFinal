package com.ltc.logisticsproject.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

// "Test datasını sıfırla" səhifəsində (bax AdminMaintenance.jsx) silmədən
// ƏVVƏL hansı hesabın hər roldan saxlanılacağını göstərmək üçün — bax
// MaintenanceService#previewKeptAccounts.
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KeptAccountPreview {
    String role;
    Long id;
    String username;
    String fullName;
    // Yalnız CUSTOMER üçün doldurulur — şifrə sıfırlama endpoint-i (bax
    // AdminManagementController#resetCustomerPassword) User.id yox, Customer.id
    // gözləyir (Driver-lə eyni "plain Long id" konvensiyası).
    Long customerId;
}
