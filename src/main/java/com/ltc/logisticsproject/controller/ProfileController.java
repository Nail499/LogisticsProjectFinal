package com.ltc.logisticsproject.controller;

import com.ltc.logisticsproject.dto.profile.CredentialsUpdateRequest;
import com.ltc.logisticsproject.dto.profile.EmailChangeConfirmRequest;
import com.ltc.logisticsproject.dto.profile.EmailChangeRequest;
import com.ltc.logisticsproject.dto.profile.ProfileResponse;
import com.ltc.logisticsproject.dto.profile.ProfileUpdateRequest;
import com.ltc.logisticsproject.entity.Customer;
import com.ltc.logisticsproject.entity.Driver;
import com.ltc.logisticsproject.entity.Role;
import com.ltc.logisticsproject.entity.User;
import com.ltc.logisticsproject.entity.VerificationPurpose;
import com.ltc.logisticsproject.repository.CustomerRepository;
import com.ltc.logisticsproject.repository.DriverRepository;
import com.ltc.logisticsproject.repository.UserRepository;
import com.ltc.logisticsproject.service.EmailVerificationService;
import com.ltc.logisticsproject.service.FileStorageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

// Stage 9 — self-service "my profile" settings for every role. Not gated by
// a role-specific path prefix on purpose (see SecurityConfig — falls under
// the generic `.anyRequest().authenticated()` rule), since every role needs
// the same "who am I / change my own credentials" surface.
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProfileController {

    final UserRepository userRepository;
    final CustomerRepository customerRepository;
    final DriverRepository driverRepository;
    final PasswordEncoder passwordEncoder;
    final FileStorageService fileStorageService;
    final EmailVerificationService emailVerificationService;

    @GetMapping
    public ResponseEntity<ProfileResponse> me(Authentication authentication) {
        User user = currentUser(authentication);
        return ResponseEntity.ok(toResponse(user));
    }

    @PutMapping
    public ResponseEntity<ProfileResponse> update(@RequestBody ProfileUpdateRequest request, Authentication authentication) {
        User user = currentUser(authentication);

        if (user.getRole() == Role.CUSTOMER) {
            Customer customer = customerRepository.findById(user.getCustomerId())
                    .orElseThrow(() -> new RuntimeException("Müştəri tapılmadı"));
            if (request.getFullName() != null) customer.setFullName(request.getFullName());
            customer.setDateOfBirth(request.getDateOfBirth());
            customer.setNationality(request.getNationality());
            customer.setLocation(request.getLocation());
            customerRepository.save(customer);
        } else if (user.getRole() == Role.DRIVER) {
            Driver driver = driverRepository.findById(user.getDriverId())
                    .orElseThrow(() -> new RuntimeException("Sürücü tapılmadı"));
            if (request.getFullName() != null) driver.setFullName(request.getFullName());
            driver.setDateOfBirth(request.getDateOfBirth());
            driver.setNationality(request.getNationality());
            driver.setLocation(request.getLocation());
            driverRepository.save(driver);
        } else {
            // ADMIN / DISPATCHER — no linked Customer/Driver row, fullName
            // lives directly on User. The extra fields (photo/DOB/etc.)
            // are intentionally not offered to these roles.
            if (request.getFullName() != null) user.setFullName(request.getFullName());
            userRepository.save(user);
        }

        return ResponseEntity.ok(toResponse(currentUser(authentication)));
    }

    @PutMapping("/credentials")
    public ResponseEntity<?> updateCredentials(@RequestBody CredentialsUpdateRequest request, Authentication authentication) {
        User user = currentUser(authentication);

        if (request.getCurrentPassword() == null || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.status(400).body(Map.of("message", "Cari şifrə yanlışdır"));
        }

        if (request.getNewUsername() != null && !request.getNewUsername().isBlank()
                && !request.getNewUsername().equals(user.getUsername())) {
            if (userRepository.findByUsername(request.getNewUsername()).isPresent()) {
                return ResponseEntity.status(400).body(Map.of("message", "Bu istifadəçi adı artıq mövcuddur"));
            }
            user.setUsername(request.getNewUsername());
        }

        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        userRepository.save(user);
        return ResponseEntity.ok(toResponse(user));
    }

    // Email dəyişmə axınının 1-ci addımı — ADMIN xaric bütün rollar üçün.
    // Yeni email dərhal yazılmır: kod həmin ünvana göndərilir, User.pendingEmail-də
    // saxlanılır, yalnız confirmEmailChange-də (doğru kodla) əsl sahəyə köçürülür.
    @PostMapping("/email/request-change")
    public ResponseEntity<?> requestEmailChange(@RequestBody EmailChangeRequest request, Authentication authentication) {
        User user = currentUser(authentication);

        if (user.getRole() == Role.ADMIN) {
            return ResponseEntity.status(400).body(Map.of("message", "Admin hesabı üçün email dəyişdirilə bilməz"));
        }
        String newEmail = request.getNewEmail() == null ? null : request.getNewEmail().trim();
        if (newEmail == null || newEmail.isBlank() || !newEmail.contains("@")) {
            return ResponseEntity.status(400).body(Map.of("message", "Düzgün email ünvanı daxil edin"));
        }

        user.setPendingEmail(newEmail);
        emailVerificationService.generateAndSend(user, newEmail, VerificationPurpose.EMAIL_CHANGE);
        return ResponseEntity.ok(Map.of("message", "Təsdiq kodu yeni email ünvanına göndərildi"));
    }

    // 2-ci (təsdiq) addım — kod doğrudursa pendingEmail rola görə əsl sahəyə
    // (Customer.email/Driver.email/User.email) yazılır.
    @PostMapping("/email/confirm-change")
    public ResponseEntity<?> confirmEmailChange(@RequestBody EmailChangeConfirmRequest request, Authentication authentication) {
        User user = currentUser(authentication);

        if (user.getPendingEmail() == null || user.getPendingEmail().isBlank()) {
            return ResponseEntity.status(400).body(Map.of("message", "Əvvəlcə yeni email üçün kod tələb edin"));
        }

        emailVerificationService.verify(user, request.getCode(), VerificationPurpose.EMAIL_CHANGE);

        String confirmedEmail = user.getPendingEmail();
        user.setPendingEmail(null);

        if (user.getRole() == Role.CUSTOMER && user.getCustomerId() != null) {
            Customer customer = customerRepository.findById(user.getCustomerId())
                    .orElseThrow(() -> new RuntimeException("Müştəri tapılmadı"));
            customer.setEmail(confirmedEmail);
            customerRepository.save(customer);
            userRepository.save(user);
        } else if (user.getRole() == Role.DRIVER && user.getDriverId() != null) {
            Driver driver = driverRepository.findById(user.getDriverId())
                    .orElseThrow(() -> new RuntimeException("Sürücü tapılmadı"));
            driver.setEmail(confirmedEmail);
            driverRepository.save(driver);
            userRepository.save(user);
        } else {
            user.setEmail(confirmedEmail);
            userRepository.save(user);
        }

        return ResponseEntity.ok(toResponse(currentUser(authentication)));
    }

    // Profile photo — CUSTOMER/DRIVER only, mirrors the driver "proof of
    // delivery" upload pattern in DriverController (same FileStorageService).
    @PostMapping(value = "/photo", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadPhoto(@RequestParam("photo") MultipartFile photo, Authentication authentication) {
        User user = currentUser(authentication);
        String url = fileStorageService.store(photo);

        if (user.getRole() == Role.CUSTOMER) {
            Customer customer = customerRepository.findById(user.getCustomerId())
                    .orElseThrow(() -> new RuntimeException("Müştəri tapılmadı"));
            customer.setPhotoUrl(url);
            customerRepository.save(customer);
        } else if (user.getRole() == Role.DRIVER) {
            Driver driver = driverRepository.findById(user.getDriverId())
                    .orElseThrow(() -> new RuntimeException("Sürücü tapılmadı"));
            driver.setPhotoUrl(url);
            driverRepository.save(driver);
        } else {
            return ResponseEntity.status(400).body(Map.of("message", "Bu rol üçün profil şəkli dəstəklənmir"));
        }

        return ResponseEntity.ok(toResponse(currentUser(authentication)));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("İstifadəçi tapılmadı"));
    }

    private ProfileResponse toResponse(User user) {
        ProfileResponse.ProfileResponseBuilder builder = ProfileResponse.builder()
                .username(user.getUsername())
                .role(user.getRole());

        if (user.getRole() == Role.CUSTOMER && user.getCustomerId() != null) {
            customerRepository.findById(user.getCustomerId()).ifPresent(c -> builder
                    .fullName(c.getFullName())
                    .photoUrl(c.getPhotoUrl())
                    .dateOfBirth(c.getDateOfBirth())
                    .nationality(c.getNationality())
                    .location(c.getLocation())
                    .email(c.getEmail()));
        } else if (user.getRole() == Role.DRIVER && user.getDriverId() != null) {
            driverRepository.findById(user.getDriverId()).ifPresent(d -> builder
                    .fullName(d.getFullName())
                    .photoUrl(d.getPhotoUrl())
                    .dateOfBirth(d.getDateOfBirth())
                    .nationality(d.getNationality())
                    .location(d.getLocation())
                    .email(d.getEmail()));
        } else {
            builder.fullName(user.getFullName());
            // DISPATCHER üçün email User-in özündə saxlanılır; ADMIN üçün
            // bu sahə həmişə null qalır (frontend "Admin" rolunda Email
            // bölməsini göstərmir).
            if (user.getRole() == Role.DISPATCHER) {
                builder.email(user.getEmail());
            }
        }

        return builder.build();
    }
}
