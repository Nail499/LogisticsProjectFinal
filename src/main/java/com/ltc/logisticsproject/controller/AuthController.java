package com.ltc.logisticsproject.controller;

import com.ltc.logisticsproject.dto.LoginRequest;
import com.ltc.logisticsproject.dto.LoginResponse;
import com.ltc.logisticsproject.dto.RegisterCustomerRequest;
import com.ltc.logisticsproject.entity.Customer;
import com.ltc.logisticsproject.entity.Role;
import com.ltc.logisticsproject.entity.User;
import com.ltc.logisticsproject.repository.CustomerRepository;
import com.ltc.logisticsproject.repository.UserRepository;
import com.ltc.logisticsproject.security.JwtUtil;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthController {

    final AuthenticationManager authenticationManager;
    final UserRepository userRepository;
    final CustomerRepository customerRepository;
    final PasswordEncoder passwordEncoder;
    final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("İstifadəçi tapılmadı"));

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());

        return ResponseEntity.ok(new LoginResponse(token, user.getRole().name(), user.getUsername()));
    }

    @PostMapping("/register/customer")
    public ResponseEntity<?> registerCustomer(@RequestBody RegisterCustomerRequest request) {

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Bu istifadəçi adı artıq mövcuddur");
        }

        Customer customer = Customer.builder()
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .companyName(request.getCompanyName())
                .build();
        customer = customerRepository.save(customer);

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.CUSTOMER)
                .customerId(customer.getId())
                .enabled(true)
                .build();
        userRepository.save(user);

        return ResponseEntity.ok("Qeydiyyat uğurludur, indi login ola bilərsiniz");
    }
}