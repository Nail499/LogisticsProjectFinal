package com.ltc.logisticsproject.controller;

import com.ltc.logisticsproject.dto.ApprovalResponse;
import com.ltc.logisticsproject.dto.RejectRequest;
import com.ltc.logisticsproject.entity.JobApplication;
import com.ltc.logisticsproject.entity.User;
import com.ltc.logisticsproject.repository.JobApplicationRepository;
import com.ltc.logisticsproject.repository.UserRepository;
import com.ltc.logisticsproject.service.AdminApplicationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/applications")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminApplicationController {

    final JobApplicationRepository jobApplicationRepository;
    final UserRepository userRepository;
    final AdminApplicationService adminApplicationService;

    @GetMapping
    public ResponseEntity<List<JobApplication>> getAll() {
        return ResponseEntity.ok(jobApplicationRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobApplication> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(jobApplicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Müraciət tapılmadı")));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApprovalResponse> approve(@PathVariable Long id, Authentication authentication) {
        Long adminId = currentUserId(authentication);
        return ResponseEntity.ok(adminApplicationService.approve(id, adminId));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<String> reject(@PathVariable Long id, @RequestBody RejectRequest request, Authentication authentication) {
        Long adminId = currentUserId(authentication);
        adminApplicationService.reject(id, request.getRejectionReason(), adminId);
        return ResponseEntity.ok("Müraciət rədd edildi");
    }

    private Long currentUserId(Authentication authentication) {
        User admin = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Admin tapılmadı"));
        return admin.getId();
    }
}