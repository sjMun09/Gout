package com.gout.controller;

import com.gout.config.openapi.AuthenticatedApiResponses;
import com.gout.config.openapi.PublicApiResponses;
import com.gout.dto.request.CreatePostRequest;
import com.gout.global.page.PageablePolicy;
import com.gout.dto.response.PostDetailResponse;
import com.gout.dto.response.PostSummaryResponse;
import com.gout.global.response.ApiResponse;
import com.gout.global.response.ErrorResponse;
import com.gout.security.CurrentUserProvider;
import com.gout.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Post", description = "커뮤니티 게시글 — 목록/상세/작성/수정/삭제/좋아요. 조회는 공개, 쓰기는 인증 필요.")
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final CurrentUserProvider currentUserProvider;

    /**
     * days/limit 이 허용 범위 밖일 때 예외 대신 기본값으로 clamp — API 계층 방어 처리.
     * 비즈니스 로직은 서비스에 위임하지 않고 여기서만 처리한다.
     */
    @Operation(
            summary = "인기 게시글 조회",
            description = "최근 N일 내 좋아요 수 기준 상위 게시글. days/limit 은 허용 범위 밖이면 기본값으로 clamp.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "인기 게시글 목록.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    @PublicApiResponses
    @SecurityRequirements({})
    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<List<PostSummaryResponse>>> trending(
            @Parameter(description = "조회 윈도우(일). 1~30 외 값은 7로 clamp.", example = "7")
            @RequestParam(defaultValue = "7") int days,
            @Parameter(description = "반환 개수. 1~20 외 값은 5로 clamp.", example = "5")
            @RequestParam(defaultValue = "5") int limit) {
        int safeDays = (days < 1 || days > 30) ? 7 : days;
        int safeLimit = (limit < 1 || limit > 20) ? 5 : limit;
        return ResponseEntity.ok(
                ApiResponse.success(postService.getTrending(safeDays, safeLimit)));
    }

    @Operation(
            summary = "게시글 목록",
            description = "카테고리/키워드/태그/정렬 기준 게시글 페이지를 반환한다. size 는 상한이 적용된다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "게시글 페이지.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    @PublicApiResponses
    @SecurityRequirements({})
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PostSummaryResponse>>> list(
            @Parameter(description = "카테고리 필터(예: GENERAL/QUESTION). 미지정 시 전체.")
            @RequestParam(required = false) String category,
            @Parameter(description = "검색 키워드(제목/본문 부분 일치).")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "정렬 키. 예: latest, popular.", example = "latest")
            @RequestParam(required = false) String sort,
            @Parameter(description = "태그 필터(단일 태그명).")
            @RequestParam(required = false) String tag,
            @Parameter(description = "0-based 페이지 번호.", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기. 상한 초과 시 자동 clamp.", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        // 메모리·네트워크 DoS 방어: PageablePolicy.POST 가 page/size 를 동시에 보정.
        int safePage = PageablePolicy.POST.clampPage(page);
        int safeSize = PageablePolicy.POST.clampSize(size);
        return ResponseEntity.ok(
                ApiResponse.success(postService.getPosts(category, keyword, sort, tag, safePage, safeSize)));
    }

    @Operation(
            summary = "게시글 상세",
            description = "단일 게시글 본문/통계/내 좋아요 여부를 조회. 비로그인 호출도 허용.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "게시글 상세.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "게시글을 찾을 수 없음.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "POST_NOT_FOUND",
                                    value = "{\"success\":false,\"code\":\"COMMUNITY_POST_NOT_FOUND\",\"message\":\"게시글을 찾을 수 없습니다.\",\"status\":404,\"path\":\"/api/posts/abc\",\"timestamp\":\"2026-01-01T00:00:00Z\"}")))
    })
    @PublicApiResponses
    @SecurityRequirements({})
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> detail(
            @Parameter(description = "게시글 ID.", example = "01HABCDEF...")
            @PathVariable String id) {
        String userId = currentUserProvider.findUserId().orElse(null);
        return ResponseEntity.ok(ApiResponse.success(postService.getPost(id, userId)));
    }

    @Operation(summary = "게시글 작성", description = "제목/본문/카테고리/태그를 받아 새 게시글을 생성한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "작성 성공.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    @AuthenticatedApiResponses
    @PostMapping
    public ResponseEntity<ApiResponse<PostSummaryResponse>> create(
            @Valid @RequestBody CreatePostRequest request) {
        String userId = currentUserProvider.requireUserId();
        return ResponseEntity.ok(
                ApiResponse.success("게시글이 작성되었습니다.", postService.createPost(userId, request)));
    }

    @Operation(summary = "게시글 수정", description = "본인이 작성한 게시글의 제목/본문/카테고리/태그를 수정한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수정 성공.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "수정 대상 게시글이 존재하지 않음.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "POST_NOT_FOUND",
                                    value = "{\"success\":false,\"code\":\"COMMUNITY_POST_NOT_FOUND\",\"message\":\"게시글을 찾을 수 없습니다.\",\"status\":404,\"path\":\"/api/posts/abc\",\"timestamp\":\"2026-01-01T00:00:00Z\"}")))
    })
    @AuthenticatedApiResponses
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> update(
            @Parameter(description = "게시글 ID.") @PathVariable String id,
            @Valid @RequestBody CreatePostRequest request) {
        String userId = currentUserProvider.requireUserId();
        return ResponseEntity.ok(
                ApiResponse.success("게시글이 수정되었습니다.", postService.updatePost(id, userId, request)));
    }

    @Operation(summary = "게시글 삭제", description = "본인이 작성한 게시글을 삭제한다(soft delete).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "삭제 성공.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "삭제 대상 게시글이 존재하지 않음.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "POST_NOT_FOUND",
                                    value = "{\"success\":false,\"code\":\"COMMUNITY_POST_NOT_FOUND\",\"message\":\"게시글을 찾을 수 없습니다.\",\"status\":404,\"path\":\"/api/posts/abc\",\"timestamp\":\"2026-01-01T00:00:00Z\"}")))
    })
    @AuthenticatedApiResponses
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "게시글 ID.") @PathVariable String id) {
        String userId = currentUserProvider.requireUserId();
        postService.deletePost(id, userId);
        return ResponseEntity.ok(ApiResponse.success("게시글이 삭제되었습니다.", null));
    }

    @Operation(summary = "게시글 좋아요 토글", description = "현재 사용자의 좋아요를 toggle 한다(이미 눌렸으면 취소).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "좋아요 상태 변경됨.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "좋아요 대상 게시글이 존재하지 않음.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "POST_NOT_FOUND",
                                    value = "{\"success\":false,\"code\":\"COMMUNITY_POST_NOT_FOUND\",\"message\":\"게시글을 찾을 수 없습니다.\",\"status\":404,\"path\":\"/api/posts/abc/like\",\"timestamp\":\"2026-01-01T00:00:00Z\"}")))
    })
    @AuthenticatedApiResponses
    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<Void>> toggleLike(
            @Parameter(description = "게시글 ID.") @PathVariable String id) {
        String userId = currentUserProvider.requireUserId();
        postService.toggleLike(id, userId);
        return ResponseEntity.ok(ApiResponse.success("좋아요 상태가 변경되었습니다.", null));
    }
}
