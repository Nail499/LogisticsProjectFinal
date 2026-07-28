package com.ltc.logisticsproject.repository;

import com.ltc.logisticsproject.entity.Role;
import com.ltc.logisticsproject.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByDriverId(Long driverId);
    Optional<User> findByCustomerId(Long customerId);
    List<User> findByRole(Role role);
}
