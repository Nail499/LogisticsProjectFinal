package com.ltc.logisticsproject.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

// Admin panelindəki kritik əməliyyatların (yaratma/silmə/dəyişdirmə)
// tarixçəsi — "kim, nə vaxt, nə etdi" sualına cavab üçün. AOP əvəzinə
// qəsdən sadə, birbaşa servis çağırışı istifadə olunur (bax
// AuditLogService#log) — layihədə artıq AOP istifadə edilmədiyi üçün yeni
// asılılıq/proxy mürəkkəbliyi əlavə etməmək məqsədilə, və hər əməliyyatın
// nə qeyd olunacağı üzərində tam nəzarət saxlamaq üçün.
@Entity
@Table(name = "audit_logs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    String actorUsername;

    String actorRole;

    @Column(nullable = false)
    String action;

    String entityType;

    @Column(length = 1000)
    String details;

    @Column(nullable = false)
    LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
