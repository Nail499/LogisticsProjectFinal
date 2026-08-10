package com.ltc.logisticsproject.repository;

import com.ltc.logisticsproject.entity.CustomsDeclaration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomsDeclarationRepository extends JpaRepository<CustomsDeclaration, Long> {
    Optional<CustomsDeclaration> findByCargoId(Long cargoId);
}
