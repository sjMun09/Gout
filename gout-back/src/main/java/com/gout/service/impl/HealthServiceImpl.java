package com.gout.service.impl;

import com.gout.dao.GoutAttackLogRepository;
import com.gout.dao.MedicationLogRepository;
import com.gout.dao.UricAcidLogRepository;
import com.gout.dto.request.CreateGoutAttackLogRequest;
import com.gout.dto.request.CreateMedicationLogRequest;
import com.gout.dto.request.CreateUricAcidLogRequest;
import com.gout.dto.response.GoutAttackLogResponse;
import com.gout.dto.response.MedicationLogResponse;
import com.gout.dto.response.UricAcidLogResponse;
import com.gout.entity.GoutAttackLog;
import com.gout.entity.MedicationLog;
import com.gout.entity.UricAcidLog;
import com.gout.global.exception.BusinessException;
import com.gout.global.exception.ErrorCode;
import com.gout.service.HealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HealthServiceImpl implements HealthService {

    private final UricAcidLogRepository uricAcidLogRepository;
    private final GoutAttackLogRepository goutAttackLogRepository;
    private final MedicationLogRepository medicationLogRepository;

    // ===== 요산수치 =====

    @Override
    @Transactional(readOnly = true)
    public List<UricAcidLogResponse> getUricAcidLogs(String userId) {
        return uricAcidLogRepository.findByUserIdOrderByMeasuredAtDesc(userId).stream()
                .map(UricAcidLogResponse::of)
                .toList();
    }

    @Override
    @Transactional
    public UricAcidLogResponse createUricAcidLog(String userId, CreateUricAcidLogRequest request) {
        UricAcidLog log = UricAcidLog.builder()
                .userId(userId)
                .value(request.getValue())
                .measuredAt(request.getMeasuredAt())
                .memo(request.getMemo())
                .build();
        return UricAcidLogResponse.of(uricAcidLogRepository.save(log));
    }

    @Override
    @Transactional
    public void deleteUricAcidLog(String id, String userId) {
        UricAcidLog log = uricAcidLogRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.HEALTH_LOG_NOT_FOUND));

        if (!log.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        uricAcidLogRepository.delete(log);
    }

    // ===== 통풍발작 =====

    @Override
    @Transactional(readOnly = true)
    public List<GoutAttackLogResponse> getGoutAttackLogs(String userId) {
        return goutAttackLogRepository.findByUserIdOrderByAttackedAtDesc(userId).stream()
                .map(GoutAttackLogResponse::of)
                .toList();
    }

    @Override
    @Transactional
    public GoutAttackLogResponse createGoutAttackLog(String userId, CreateGoutAttackLogRequest request) {
        GoutAttackLog log = GoutAttackLog.builder()
                .userId(userId)
                .attackedAt(request.getAttackedAt())
                .painLevel(request.getPainLevel())
                .location(request.getLocation())
                .durationDays(request.getDurationDays())
                .suspectedCause(request.getSuspectedCause())
                .memo(request.getMemo())
                .build();
        return GoutAttackLogResponse.of(goutAttackLogRepository.save(log));
    }

    @Override
    @Transactional
    public void deleteGoutAttackLog(String id, String userId) {
        GoutAttackLog log = goutAttackLogRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.HEALTH_LOG_NOT_FOUND));

        if (!log.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        goutAttackLogRepository.delete(log);
    }

    // ===== 복약 =====

    @Override
    @Transactional(readOnly = true)
    public List<MedicationLogResponse> getMedicationLogs(String userId) {
        return medicationLogRepository.findByUserIdOrderByTakenAtDesc(userId).stream()
                .map(MedicationLogResponse::of)
                .toList();
    }

    @Override
    @Transactional
    public MedicationLogResponse createMedicationLog(String userId, CreateMedicationLogRequest request) {
        MedicationLog log = MedicationLog.builder()
                .userId(userId)
                .medicationName(request.getMedicationName())
                .dosage(request.getDosage())
                .takenAt(request.getTakenAt())
                .build();
        return MedicationLogResponse.of(medicationLogRepository.save(log));
    }

    @Override
    @Transactional
    public void deleteMedicationLog(String id, String userId) {
        MedicationLog log = medicationLogRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.HEALTH_LOG_NOT_FOUND));

        if (!log.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        medicationLogRepository.delete(log);
    }
}
