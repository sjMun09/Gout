package com.gout.dao;

import com.gout.entity.MedicationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicationLogRepository extends JpaRepository<MedicationLog, String> {

    List<MedicationLog> findByUserIdOrderByTakenAtDesc(String userId);
}
