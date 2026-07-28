package com.ltc.logisticsproject.repository;

import com.ltc.logisticsproject.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication,Long> {
    Optional<JobApplication> findByApplicationCode(String applicationCode);
}
