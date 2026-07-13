package com.ltc.logisticsproject.repository;

import com.ltc.logisticsproject.entity.Driver;
import com.ltc.logisticsproject.entity.DriverStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {
    Optional<Driver> findByPhone(String phone);
    List<Driver> findByStatus(DriverStatus status);
}
