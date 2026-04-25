package com.gout.controller;

import com.gout.config.openapi.AuthenticatedApiResponses;
import com.gout.dto.request.CreateGoutAttackLogRequest;
import com.gout.dto.request.CreateMedicationLogRequest;
import com.gout.dto.request.CreateUricAcidLogRequest;
import com.gout.dto.response.GoutAttackLogResponse;
import com.gout.dto.response.MedicationLogResponse;
import com.gout.dto.response.UricAcidLogResponse;
import com.gout.global.response.ApiResponse;
import com.gout.global.response.ErrorResponse;
import com.gout.security.CurrentUserProvider;
import com.gout.service.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Health", description = "사용자 건강 기록 — 요산수치/통풍발작/복약 로그 CRUD.")
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthService healthService;
    private final CurrentUserProvider currentUserProvider;

    // ===== 요산수치 =====

    @Operation(summary = "요산수치 기록 목록", description = "현재 사용자의 모든 요산수치 측정 로그를 시간순으로 반환.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    @AuthenticatedApiResponses
    @GetMapping("/uric-acid-logs")
    public ResponseEntity<ApiResponse<List<UricAcidLogResponse>>> getUricAcidLogs() {
        return ResponseEntity.ok(ApiResponse.success(healthService.getUricAcidLogs(currentUserProvider.requireUserId())));
    }

    @Operation(summary = "요산수치 기록 생성", description = "값/측정일/메모를 받아 새 측정 로그를 생성한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "생성 성공.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    @AuthenticatedApiResponses
    @PostMapping("/uric-acid-logs")
    public ResponseEntity<ApiResponse<UricAcidLogResponse>> createUricAcidLog(
            @Valid @RequestBody CreateUricAcidLogRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "요산 수치 기록 완료",
                healthService.createUricAcidLog(currentUserProvider.requireUserId(), request)));
    }

    @Operation(summary = "요산수치 기록 삭제", description = "본인이 생성한 측정 로그만 삭제 가능.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "삭제 성공.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "기록을 찾을 수 없음.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "HEALTH_LOG_NOT_FOUND",
                                    value = "{\"success\":false,\"code\":\"HEALTH_LOG_NOT_FOUND\",\"message\":\"기록을 찾을 수 없습니다.\",\"status\":404,\"path\":\"/api/health/uric-acid-logs/abc\",\"timestamp\":\"2026-01-01T00:00:00Z\"}")))
    })
    @AuthenticatedApiResponses
    @DeleteMapping("/uric-acid-logs/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUricAcidLog(
            @Parameter(description = "요산수치 로그 ID.") @PathVariable String id) {
        healthService.deleteUricAcidLog(id, currentUserProvider.requireUserId());
        return ResponseEntity.ok(ApiResponse.success("삭제되었습니다.", null));
    }

    // ===== 통풍발작 =====

    @Operation(summary = "통풍발작 기록 목록", description = "현재 사용자의 통풍발작 로그를 시간순으로 반환.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    @AuthenticatedApiResponses
    @GetMapping("/gout-attack-logs")
    public ResponseEntity<ApiResponse<List<GoutAttackLogResponse>>> getGoutAttackLogs() {
        return ResponseEntity.ok(ApiResponse.success(healthService.getGoutAttackLogs(currentUserProvider.requireUserId())));
    }

    @Operation(summary = "통풍발작 기록 생성", description = "발생일/통증/부위 등을 받아 발작 로그를 생성한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "생성 성공.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    @AuthenticatedApiResponses
    @PostMapping("/gout-attack-logs")
    public ResponseEntity<ApiResponse<GoutAttackLogResponse>> createGoutAttackLog(
            @Valid @RequestBody CreateGoutAttackLogRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "통풍 발작 기록 완료",
                healthService.createGoutAttackLog(currentUserProvider.requireUserId(), request)));
    }

    @Operation(summary = "통풍발작 기록 삭제", description = "본인이 생성한 발작 로그만 삭제 가능.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "삭제 성공.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "삭제 대상 통풍발작 로그가 존재하지 않거나 본인 소유가 아님.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "HEALTH_LOG_NOT_FOUND",
                                    value = "{\"success\":false,\"code\":\"HEALTH_LOG_NOT_FOUND\",\"message\":\"건강 기록을 찾을 수 없습니다.\",\"status\":404,\"path\":\"/api/health/gout-attack-logs/abc\",\"timestamp\":\"2026-01-01T00:00:00Z\"}")))
    })
    @AuthenticatedApiResponses
    @DeleteMapping("/gout-attack-logs/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGoutAttackLog(
            @Parameter(description = "통풍발작 로그 ID.") @PathVariable String id) {
        healthService.deleteGoutAttackLog(id, currentUserProvider.requireUserId());
        return ResponseEntity.ok(ApiResponse.success("삭제되었습니다.", null));
    }

    // ===== 복약 =====

    @Operation(summary = "복약 기록 목록", description = "현재 사용자의 복약 기록을 시간순으로 반환.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    @AuthenticatedApiResponses
    @GetMapping("/medication-logs")
    public ResponseEntity<ApiResponse<List<MedicationLogResponse>>> getMedicationLogs() {
        return ResponseEntity.ok(ApiResponse.success(healthService.getMedicationLogs(currentUserProvider.requireUserId())));
    }

    @Operation(summary = "복약 기록 생성", description = "약 이름/복용일/메모를 받아 복약 로그를 생성한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "생성 성공.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    @AuthenticatedApiResponses
    @PostMapping("/medication-logs")
    public ResponseEntity<ApiResponse<MedicationLogResponse>> createMedicationLog(
            @Valid @RequestBody CreateMedicationLogRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "복약 기록 완료",
                healthService.createMedicationLog(currentUserProvider.requireUserId(), request)));
    }

    @Operation(summary = "복약 기록 삭제", description = "본인이 생성한 복약 로그만 삭제 가능.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "삭제 성공.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "삭제 대상 복약 로그가 존재하지 않거나 본인 소유가 아님.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "HEALTH_LOG_NOT_FOUND",
                                    value = "{\"success\":false,\"code\":\"HEALTH_LOG_NOT_FOUND\",\"message\":\"건강 기록을 찾을 수 없습니다.\",\"status\":404,\"path\":\"/api/health/medication-logs/abc\",\"timestamp\":\"2026-01-01T00:00:00Z\"}")))
    })
    @AuthenticatedApiResponses
    @DeleteMapping("/medication-logs/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMedicationLog(
            @Parameter(description = "복약 로그 ID.") @PathVariable String id) {
        healthService.deleteMedicationLog(id, currentUserProvider.requireUserId());
        return ResponseEntity.ok(ApiResponse.success("삭제되었습니다.", null));
    }
}
