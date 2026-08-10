package com.ltc.logisticsproject.service;

import com.ltc.logisticsproject.dto.ApprovalResponse;
import com.ltc.logisticsproject.entity.*;
import com.ltc.logisticsproject.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminApplicationService {

    final JobApplicationRepository jobApplicationRepository;
    final DriverRepository driverRepository;
    final VehicleRepository vehicleRepository;
    final UserRepository userRepository;
    final PasswordEncoder passwordEncoder;

    @Transactional
    public ApprovalResponse approve(Long applicationId, Long adminUserId) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Müraciət tapılmadı"));

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new RuntimeException("Bu müraciət artıq baxılıb");
        }

        Driver driver = Driver.builder()
                .fullName(application.getFullName())
                .phone(application.getPhone())
                .licenseDocumentUrl(application.getLicenseDocumentUrl())
                .status(DriverStatus.ACTIVE)
                .jobApplicationId(application.getId())
                .build();
        driver = driverRepository.save(driver);

        // Yalnız müraciətçi öz tırı ilə işləmək istəyibsə (hasOwnVehicle=true)
        // Vehicle yaradılır — DRIVER_OWNED olaraq, yalnız bu sürücüyə bağlı.
        // Əks halda sürücü tırsız təsdiqlənir, dispetçer sonra reys-be-reys
        // şirkət tırı təhkim edir (bax DispatcherController#allVehicles).
        Long vehicleId = null;
        if (Boolean.TRUE.equals(application.getHasOwnVehicle())) {
            // Nə Vehicle-də, nə də JobApplication-da artıq capacity sahəsi
            // yoxdur — tır yük daşımır, yalnız kəllə hissəsidir (bax
            // Vehicle.java-dakı izah).
            Vehicle vehicle = Vehicle.builder()
                    .plateNumber(application.getVehiclePlateNumber())
                    .brand(application.getVehicleBrand())
                    .vehicleDocumentUrl(application.getVehicleDocumentUrl())
                    .ownerType(OwnerType.DRIVER_OWNED)
                    .driver(driver)
                    .build();
            vehicle = vehicleRepository.save(vehicle);
            vehicleId = vehicle.getId();
        }

        String tempPassword = UUID.randomUUID().toString().substring(0, 8);

        User user = User.builder()
                .username(application.getPhone())
                .fullName(application.getFullName())
                .password(passwordEncoder.encode(tempPassword))
                .role(Role.DRIVER)
                .driverId(driver.getId())
                .enabled(true)
                .build();
        userRepository.save(user);

        application.setStatus(ApplicationStatus.APPROVED);
        application.setReviewedAt(LocalDateTime.now());
        application.setReviewedByAdminId(adminUserId);
        jobApplicationRepository.save(application);

        return ApprovalResponse.builder()
                .applicationId(application.getId())
                .driverId(driver.getId())
                .vehicleId(vehicleId)
                .username(user.getUsername())
                .temporaryPassword(tempPassword)
                .message(vehicleId != null ? "Sürücü hesabı yaradıldı" : "Sürücü hesabı yaradıldı (tırsız — dispetçer təhkim edəcək)")
                .build();
    }

    @Transactional
    public void reject(Long applicationId, String reason, Long adminUserId) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Müraciət tapılmadı"));

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new RuntimeException("Bu müraciət artıq baxılıb");
        }

        application.setStatus(ApplicationStatus.REJECTED);
        application.setRejectionReason(reason);
        application.setReviewedAt(LocalDateTime.now());
        application.setReviewedByAdminId(adminUserId);
        jobApplicationRepository.save(application);
    }
}