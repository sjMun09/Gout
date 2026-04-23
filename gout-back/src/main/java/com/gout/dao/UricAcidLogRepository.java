package com.gout.dao;

import com.gout.entity.UricAcidLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UricAcidLogRepository extends JpaRepository<UricAcidLog, String> {

    List<UricAcidLog> findByUserIdOrderByMeasuredAtDesc(String userId);
}
