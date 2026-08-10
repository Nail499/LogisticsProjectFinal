package com.ltc.logisticsproject.service;

import com.ltc.logisticsproject.entity.AuditLog;
import com.ltc.logisticsproject.repository.AuditLogRepository;
import com.ltc.logisticsproject.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

// Admin əməliyyatlarının tarixçəsini yazan mərkəzi nöqtə (bax
// AdminManagementController-dəki çağırışlar). Bir əməliyyatın loglanması
// uğursuz olsa belə (məs. gözlənilməz DB xətası) əsas əməliyyatı pozmamaq
// üçün try/catch ilə udulur — eyni ehtiyat NotificationService-də də
// tətbiq olunub (email göndərilməsi uğursuz olsa da əsas axın davam edir).
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditLogService {

    final AuditLogRepository auditLogRepository;
    final UserRepository userRepository;

    public void log(Authentication authentication, String action, String entityType, String details) {
        try {
            String username = authentication != null ? authentication.getName() : "system";
            String role = authentication != null
                    ? userRepository.findByUsername(username).map(u -> u.getRole().name()).orElse(null)
                    : null;
            AuditLog entry = AuditLog.builder()
                    .actorUsername(username)
                    .actorRole(role)
                    .action(action)
                    .entityType(entityType)
                    .details(details)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            // Audit tarixçəsi ikinci dərəcəlidir — yazıla bilməsə də əsas
            // admin əməliyyatı (silmə/yaratma) uğursuz olmamalıdır.
        }
    }
}
