package com.gout.service.impl;

import com.gout.dao.GoutAttackLogRepository;
import com.gout.dao.MedicationLogRepository;
import com.gout.dao.UricAcidLogRepository;
import com.gout.dao.UserRepository;
import com.gout.dto.request.CreateGoutAttackLogRequest;
import com.gout.dto.request.CreateMedicationLogRequest;
import com.gout.dto.request.CreateUricAcidLogRequest;
import com.gout.dto.response.GoutAttackLogResponse;
import com.gout.dto.response.MedicationLogResponse;
import com.gout.dto.response.UricAcidLogResponse;
import com.gout.entity.GoutAttackLog;
import com.gout.entity.MedicationLog;
import com.gout.entity.UricAcidLog;
import com.gout.entity.User;
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
    private final UserRepository userRepository;

    // ===== 요산수치 =====

    @Override
    @Transactional(readOnly = true)
    public List<UricAcidLogResponse> getUricAcidLogs(String userId) {
        requireSensitiveConsent(userId);
        return uricAcidLogRepository.findByUserIdOrderByMeasuredAtDesc(userId).stream()
                .map(UricAcidLogResponse::of)
                .toList();
    }

    @Override
    @Transactional
    public UricAcidLogResponse createUricAcidLog(String userId, CreateUricAcidLogRequest request) {
        requireSensitiveConsent(userId);
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
        requireSensitiveConsent(userId);
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
        requireSensitiveConsent(userId);
        return goutAttackLogRepository.findByUserIdOrderByAttackedAtDesc(userId).stream()
                .map(GoutAttackLogResponse::of)
                .toList();
    }

    @Override
    @Transactional
    public GoutAttackLogResponse createGoutAttackLog(String userId, CreateGoutAttackLogRequest request) {
        requireSensitiveConsent(userId);
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
        requireSensitiveConsent(userId);
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
        requireSensitiveConsent(userId);
        return medicationLogRepository.findByUserIdOrderByTakenAtDesc(userId).stream()
                .map(MedicationLogResponse::of)
                .toList();
    }

    @Override
    @Transactional
    public MedicationLogResponse createMedicationLog(String userId, CreateMedicationLogRequest request) {
        requireSensitiveConsent(userId);
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
        requireSensitiveConsent(userId);
        MedicationLog log = medicationLogRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.HEALTH_LOG_NOT_FOUND));

        if (!log.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        medicationLogRepository.delete(log);
    }

    private void requireSensitiveConsent(String userId) {
        boolean hasConsent = userRepository.findById(userId)
                .filter(user -> user.getStatus() == User.Status.ACTIVE)
                .map(User::getConsentSensitiveAt)
                .isPresent();
        if (!hasConsent) {
            throw new BusinessException(ErrorCode.HEALTH_SENSITIVE_CONSENT_REQUIRED);
        }
    }
}
