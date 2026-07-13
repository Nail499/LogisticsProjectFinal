package com.ltc.logisticsproject.controller;

import com.ltc.logisticsproject.dto.ApplicationStatusResponse;
import com.ltc.logisticsproject.entity.JobApplication;
import com.ltc.logisticsproject.repository.JobApplicationRepository;
import com.ltc.logisticsproject.service.FileStorageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JobApplicationController {

    final JobApplicationRepository jobApplicationRepository;
    final FileStorageService fileStorageService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApplicationStatusResponse> submit(
            @RequestParam String fullName,
            @RequestParam String phone,
            @RequestParam("licenseDocument") MultipartFile licenseDocument,
            @RequestParam String vehiclePlateNumber,
            @RequestParam String vehicleBrand,
            @RequestParam Double vehicleCapacity,
            @RequestParam("vehicleDocument") MultipartFile vehicleDocument
    ) {
        String licenseUrl = fileStorageService.store(licenseDocument);
        String vehicleDocUrl = fileStorageService.store(vehicleDocument);

        JobApplication application = JobApplication.builder()
                .fullName(fullName)
                .phone(phone)
                .licenseDocumentUrl(licenseUrl)
                .vehiclePlateNumber(vehiclePlateNumber)
                .vehicleBrand(vehicleBrand)
                .vehicleCapacity(vehicleCapacity)
                .vehicleDocumentUrl(vehicleDocUrl)
                .build();

        application = jobApplicationRepository.save(application);

        return ResponseEntity.ok(ApplicationStatusResponse.builder()
                .id(application.getId())
                .status(application.getStatus())
                .message("Müraciətiniz qəbul edildi, nəzərdən keçirilir")
                .build());
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<ApplicationStatusResponse> checkStatus(@PathVariable Long id) {
        JobApplication application = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Müraciət tapılmadı"));

        return ResponseEntity.ok(ApplicationStatusResponse.builder()
                .id(application.getId())
                .status(application.getStatus())
                .rejectionReason(application.getRejectionReason())
                .build());
    }
}