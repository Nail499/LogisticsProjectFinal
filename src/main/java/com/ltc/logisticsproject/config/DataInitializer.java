package com.ltc.logisticsproject.config;

import com.ltc.logisticsproject.entity.Role;
import com.ltc.logisticsproject.entity.User;
import com.ltc.logisticsproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Lokal development-də rahat olsun deyə defolt "admin123" saxlanılır,
    // AMMA production-da (bax .do/app.yaml) ADMIN_INITIAL_PASSWORD env
    // dəyişəni ilə mütləq override edilməlidir — əks halda hər kəsə məlum
    // olan bu şifrə ilə real deployment-də admin panelinə giriş açıq qalar.
    @Value("${app.admin.initial-password:admin123}")
    private String initialAdminPassword;

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode(initialAdminPassword))
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build();
            userRepository.save(admin);

            if ("admin123".equals(initialAdminPassword)) {
                System.out.println(">>> Default admin yaradıldı: admin / admin123 " +
                        "(YALNIZ LOKAL İNKİŞAF ÜÇÜN — production-da ADMIN_INITIAL_PASSWORD env dəyişənini təyin edin!)");
            } else {
                System.out.println(">>> Admin hesabı yaradıldı: admin (şifrə ADMIN_INITIAL_PASSWORD env dəyişənindən götürülüb)");
            }
        }
    }
}
