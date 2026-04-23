package com.gout.dao;

import com.gout.entity.GoutAttackLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoutAttackLogRepository extends JpaRepository<GoutAttackLog, String> {

    List<GoutAttackLog> findByUserIdOrderByAttackedAtDesc(String userId);
}
