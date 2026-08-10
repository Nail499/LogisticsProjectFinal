package com.ltc.logisticsproject.repository;

import com.ltc.logisticsproject.entity.TradeDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TradeDocumentRepository extends JpaRepository<TradeDocument, Long> {
    List<TradeDocument> findByCargoIdOrderByCreatedAtDesc(Long cargoId);
}
