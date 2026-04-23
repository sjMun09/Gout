package com.gout.service;

import com.gout.dto.request.CreateGoutAttackLogRequest;
import com.gout.dto.request.CreateMedicationLogRequest;
import com.gout.dto.request.CreateUricAcidLogRequest;
import com.gout.dto.response.GoutAttackLogResponse;
import com.gout.dto.response.MedicationLogResponse;
import com.gout.dto.response.UricAcidLogResponse;

import java.util.List;

public interface HealthService {

    // 요산수치
    List<UricAcidLogResponse> getUricAcidLogs(String userId);

    UricAcidLogResponse createUricAcidLog(String userId, CreateUricAcidLogRequest request);

    void deleteUricAcidLog(String id, String userId);

    // 통풍발작
    List<GoutAttackLogResponse> getGoutAttackLogs(String userId);

    GoutAttackLogResponse createGoutAttackLog(String userId, CreateGoutAttackLogRequest request);

    void deleteGoutAttackLog(String id, String userId);

    // 복약
    List<MedicationLogResponse> getMedicationLogs(String userId);

    MedicationLogResponse createMedicationLog(String userId, CreateMedicationLogRequest request);

    void deleteMedicationLog(String id, String userId);
}
