package com.ltc.logisticsproject.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
     Long id;

    @Column(nullable = false)
    String fullName;

    @Column(nullable = false, unique = true)
     String username;

    @Column(nullable = false)
     String password; // BCrypt ilə hash olunmuş

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
     Role role;



     Long driverId;
    Long customerId;

    // DISPATCHER hesabları üçün email (Customer/Driver-dən fərqli olaraq
    // ayrıca entity-ləri yoxdur, ona görə birbaşa User-də saxlanılır — bax
    // ProfileController). ADMIN üçün bu sahə istifadə olunmur (profil
    // səhifəsində göstərilmir).
    String email;

    // Profildə "email dəyişdir" axınının ARA vəziyyəti: istifadəçi yeni
    // email daxil edib kod tələb edəndə bu sahəyə yazılır, kod TƏSDİQ
    // olunanda əsl email sahəsinə (Customer.email/Driver.email/User.email,
    // rola görə) köçürülür və bu sahə təmizlənir. Kod göndərilib təsdiqlənənə
    // qədər köhnə email dəyişmir (bax ProfileController#requestEmailChange/
    // confirmEmailChange, EmailVerificationService).
    String pendingEmail;


    // Qeydiyyat zamanı email təsdiqlənənə qədər false qalır (bax
    // UserDetailsServiceImpl#loadUserByUsername -> .disabled(!enabled) ->
    // Spring Security girişi avtomatik bloklayır). Admin/dispetçer/sürücü
    // hesabları birbaşa true yaradılır (email-təsdiqi tələb olunmur).
    @Column(nullable = false)
     Boolean enabled = true;

    // Email təsdiq kodu (qeydiyyat) və "şifrəni unutdum" kodu — hər ikisi
    // eyni sahələri paylaşır, VerificationPurpose hansı axının kodu olduğunu
    // ayırır. Kod istifadə olunandan/vaxtı bitəndən sonra null-a çevrilir.
    String verificationCode;
    LocalDateTime verificationCodeExpiresAt;
    @Enumerated(EnumType.STRING)
    VerificationPurpose verificationPurpose;

     LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.enabled == null) this.enabled = true;
    }
}