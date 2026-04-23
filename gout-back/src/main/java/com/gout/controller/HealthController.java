package com.gout.controller;

import com.gout.dto.request.CreateGoutAttackLogRequest;
import com.gout.dto.request.CreateMedicationLogRequest;
import com.gout.dto.request.CreateUricAcidLogRequest;
import com.gout.dto.response.GoutAttackLogResponse;
import com.gout.dto.response.MedicationLogResponse;
import com.gout.dto.response.UricAcidLogResponse;
import com.gout.global.response.ApiResponse;
import com.gout.service.HealthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthService healthService;

    // ===== 요산수치 =====

    @GetMapping("/uric-acid-logs")
    public ResponseEntity<ApiResponse<List<UricAcidLogResponse>>> getUricAcidLogs() {
        return ResponseEntity.ok(ApiResponse.success(healthService.getUricAcidLogs(currentUserId())));
    }

    @PostMapping("/uric-acid-logs")
    public ResponseEntity<ApiResponse<UricAcidLogResponse>> createUricAcidLog(
            @Valid @RequestBody CreateUricAcidLogRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "요산 수치 기록 완료",
                healthService.createUricAcidLog(currentUserId(), request)));
    }

    @DeleteMapping("/uric-acid-logs/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUricAcidLog(@PathVariable String id) {
        healthService.deleteUricAcidLog(id, currentUserId());
        return ResponseEntity.ok(ApiResponse.success("삭제되었습니다.", null));
    }

    // ===== 통풍발작 =====

    @GetMapping("/gout-attack-logs")
    public ResponseEntity<ApiResponse<List<GoutAttackLogResponse>>> getGoutAttackLogs() {
        return ResponseEntity.ok(ApiResponse.success(healthService.getGoutAttackLogs(currentUserId())));
    }

    @PostMapping("/gout-attack-logs")
    public ResponseEntity<ApiResponse<GoutAttackLogResponse>> createGoutAttackLog(
            @Valid @RequestBody CreateGoutAttackLogRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "통풍 발작 기록 완료",
                healthService.createGoutAttackLog(currentUserId(), request)));
    }

    @DeleteMapping("/gout-attack-logs/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGoutAttackLog(@PathVariable String id) {
        healthService.deleteGoutAttackLog(id, currentUserId());
        return ResponseEntity.ok(ApiResponse.success("삭제되었습니다.", null));
    }

    // ===== 복약 =====

    @GetMapping("/medication-logs")
    public ResponseEntity<ApiResponse<List<MedicationLogResponse>>> getMedicationLogs() {
        return ResponseEntity.ok(ApiResponse.success(healthService.getMedicationLogs(currentUserId())));
    }

    @PostMapping("/medication-logs")
    public ResponseEntity<ApiResponse<MedicationLogResponse>> createMedicationLog(
            @Valid @RequestBody CreateMedicationLogRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "복약 기록 완료",
                healthService.createMedicationLog(currentUserId(), request)));
    }

    @DeleteMapping("/medication-logs/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMedicationLog(@PathVariable String id) {
        healthService.deleteMedicationLog(id, currentUserId());
        return ResponseEntity.ok(ApiResponse.success("삭제되었습니다.", null));
    }

    private String currentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
