package com.gout;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 게시글 이미지 업로드 통합 테스트.
 *
 * <p>원래 {@link PostIntegrationTest} 는 registerAndLogin() 이
 * 기존 gender_type 컬럼 버그(app 쪽 Hibernate 매핑 이슈)로 500 을 반환해 모두 @Disabled 상태다.
 * 여기서는 JWT 대신 SecurityMockMvcRequestPostProcessors.user() 로 SecurityContext 를
 * 직접 주입하고, 게시글을 저장할 때 필요한 users row 는 JdbcTemplate 로 직접 삽입한다.</p>
 */
class PostImageUploadIntegrationTest extends IntegrationTestBase {

    @Autowired
    JdbcTemplate jdbcTemplate;

    /** 1x1 투명 PNG (base64). */
    private static final byte[] ONE_PX_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
    );

    private String insertUserRow(String email, String nickname) {
        String userId = java.util.UUID.randomUUID().toString();
        // users: id / email / password(nullable) / nickname(NOT NULL) / role(default USER) / gender enum(nullable)
        // Hibernate 의 gender_type 매핑 버그를 피하려면 JDBC 레벨에서 직접 INSERT 한다.
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password, nickname) VALUES (?, ?, ?, ?)",
                userId, email, "$2a$10$testplaceholder........................", nickname);
        return userId;
    }

    @Test
    @DisplayName("비인증 업로드는 401/403")
    void upload_requires_auth() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files", "test.png", "image/png", ONE_PX_PNG);

        mockMvc.perform(multipart("/api/uploads/posts").file(file))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("이미지 업로드 → URL 반환 → 해당 URL 로 GET 시 파일 스트리밍")
    void upload_then_download() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files", "pixel.png", "image/png", ONE_PX_PNG);

        MvcResult uploadResult = mockMvc.perform(
                        multipart("/api/uploads/posts").file(file).with(user("test-user-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.urls").isArray())
                .andExpect(jsonPath("$.data.urls[0]").exists())
                .andReturn();

        JsonNode body = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        String url = body.path("data").path("urls").get(0).asText();
        assertNotNull(url);
        assertTrue(url.startsWith("/api/uploads/posts/"), "URL 은 /api/uploads/posts/ 로 시작해야 함: " + url);

        // GET — 공개
        MvcResult getResult = mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn();
        byte[] served = getResult.getResponse().getContentAsByteArray();
        assertEquals(ONE_PX_PNG.length, served.length,
                "서빙된 파일 크기가 업로드한 것과 일치해야 함");
    }

    @Test
    @DisplayName("허용되지 않은 MIME 타입은 400")
    void rejects_bad_mime() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files", "evil.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/api/uploads/posts").file(file).with(user("test-user-2")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("MED-003: Content-Type 은 image/png 이지만 실제 바이트는 비-PNG → 4xx")
    void rejects_magic_byte_mismatch() throws Exception {
        // 헤더는 image/png 라고 주장하지만 바디는 평문 → 매직바이트 검증에서 거부.
        MockMultipartFile file = new MockMultipartFile(
                "files", "fake.png", "image/png", "definitely not a real png".getBytes());

        mockMvc.perform(multipart("/api/uploads/posts").file(file).with(user("test-user-magic")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("MED-003: 서빙 응답에 X-Content-Type-Options: nosniff 헤더 포함")
    void serve_includes_nosniff_header() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files", "pixel.png", "image/png", ONE_PX_PNG);

        MvcResult uploadResult = mockMvc.perform(
                        multipart("/api/uploads/posts").file(file).with(user("test-user-nosniff")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        String url = body.path("data").path("urls").get(0).asText();

        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    @DisplayName("게시글 작성 시 imageUrls 배열이 저장/응답에 포함")
    void create_post_with_image_urls() throws Exception {
        String userId = insertUserRow("imgpost@gout.test", "이미지작성자");

        // 1. 업로드
        MockMultipartFile file = new MockMultipartFile(
                "files", "a.png", "image/png", ONE_PX_PNG);
        MvcResult upRes = mockMvc.perform(
                        multipart("/api/uploads/posts").file(file).with(user(userId)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode upBody = objectMapper.readTree(upRes.getResponse().getContentAsString());
        String url = upBody.path("data").path("urls").get(0).asText();

        // 2. 게시글 작성 — imageUrls 포함
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(("{\"title\":\"이미지 글\",\"content\":\"본문\"," +
                "\"category\":\"FREE\",\"isAnonymous\":false," +
                "\"imageUrls\":[\"" + url + "\"]}").getBytes());

        MvcResult createRes = mockMvc.perform(post("/api/posts")
                        .with(user(userId))
                        .contentType("application/json")
                        .content(bos.toByteArray()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imageUrls").isArray())
                .andExpect(jsonPath("$.data.imageUrls[0]").value(url))
                .andReturn();

        JsonNode createBody = objectMapper.readTree(createRes.getResponse().getContentAsString());
        String postId = createBody.path("data").path("id").asText();
        assertNotNull(postId);
        assertTrue(!postId.isEmpty());

        // 3. 상세 조회 시에도 imageUrls 내려옴
        mockMvc.perform(get("/api/posts/" + postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imageUrls").isArray())
                .andExpect(jsonPath("$.data.imageUrls[0]").value(url));
    }
}
