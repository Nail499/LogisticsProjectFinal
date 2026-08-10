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

    // Tır sahələri artıq MƏCBURİ deyil — bəzi müraciətçilərin şirkət tırı ilə
    // işləməsi lazımdır (bax JobApplication.hasOwnVehicle izahı). hasOwnVehicle
    // true-dursa vehicle* sahələri (plaka, marka, sənəd) tələb olunur, əks
    // halda hamısı boş buraxıla bilər. Tutum (capacity) sahəsi qəsdən YOXDUR —
    // tır yük daşımır, yalnız kəllə hissəsidir (bax Vehicle.java izahı).
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> submit(
            @RequestParam String fullName,
            @RequestParam String phone,
            @RequestParam("licenseDocument") MultipartFile licenseDocument,
            @RequestParam(required = false, defaultValue = "false") boolean hasOwnVehicle,
            @RequestParam(required = false) String vehiclePlateNumber,
            @RequestParam(required = false) String vehicleBrand,
            @RequestParam(value = "vehicleDocument", required = false) MultipartFile vehicleDocument
    ) {
        if (hasOwnVehicle) {
            boolean missing = vehiclePlateNumber == null || vehiclePlateNumber.isBlank()
                    || vehicleBrand == null || vehicleBrand.isBlank()
                    || vehicleDocument == null || vehicleDocument.isEmpty();
            if (missing) {
                return ResponseEntity.badRequest().body(java.util.Map.of(
                        "message", "Öz tırınızla müraciət edirsinizsə, tır məlumatları və sənədi tələb olunur"));
            }
        }

        String licenseUrl = fileStorageService.store(licenseDocument);
        String vehicleDocUrl = (hasOwnVehicle && vehicleDocument != null && !vehicleDocument.isEmpty())
                ? fileStorageService.store(vehicleDocument) : null;

        JobApplication application = JobApplication.builder()
                .fullName(fullName)
                .phone(phone)
                .licenseDocumentUrl(licenseUrl)
                .hasOwnVehicle(hasOwnVehicle)
                .vehiclePlateNumber(hasOwnVehicle ? vehiclePlateNumber : null)
                .vehicleBrand(hasOwnVehicle ? vehicleBrand : null)
                .vehicleDocumentUrl(vehicleDocUrl)
                .build();

        application = jobApplicationRepository.save(application);

        return ResponseEntity.ok(ApplicationStatusResponse.builder()
                .applicationCode(application.getApplicationCode())
                .status(application.getStatus())
                .message("Müraciətiniz qəbul edildi, nəzərdən keçirilir")
                .build());
    }

    // Path dəyişkəni "id" deyil "code" — bax JobApplication.applicationCode
    // izahı: ardıcıl DB id-si əvəzinə unikal kodla axtarılır (həm daha
    // "peşəkar" görünür, həm də enumerasiya təhlükəsizlik riskini aradan qaldırır).
    @GetMapping("/status/{code}")
    public ResponseEntity<ApplicationStatusResponse> checkStatus(@PathVariable String code) {
        JobApplication application = jobApplicationRepository.findByApplicationCode(code)
                .orElseThrow(() -> new RuntimeException("Müraciət tapılmadı"));

        return ResponseEntity.ok(ApplicationStatusResponse.builder()
                .applicationCode(application.getApplicationCode())
                .status(application.getStatus())
                .rejectionReason(application.getRejectionReason())
                .build());
    }
}