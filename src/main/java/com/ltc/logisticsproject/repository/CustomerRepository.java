package com.ltc.logisticsproject.repository;

import com.ltc.logisticsproject.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer,Long> {
    Optional<Customer> findByPhone(String phone);

    // "Şifrəni unutdum" axını üçün — Customer.email unikallıq məhdudiyyəti
    // olmadan mövcuddur (köhnə qeydlərdə boş/təkrar ola bilər deyə DB
    // səviyyəsində unique əlavə edilmədi), ona görə ilk uyğun gələni götürür.
    Optional<Customer> findFirstByEmail(String email);
}
