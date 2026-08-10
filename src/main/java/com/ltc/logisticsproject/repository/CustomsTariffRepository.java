package com.ltc.logisticsproject.repository;

import com.ltc.logisticsproject.entity.CargoType;
import com.ltc.logisticsproject.entity.CustomsTariff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomsTariffRepository extends JpaRepository<CustomsTariff, Long> {
    Optional<CustomsTariff> findByCargoType(CargoType cargoType);
}
