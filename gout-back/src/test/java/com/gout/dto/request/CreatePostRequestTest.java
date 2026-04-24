package com.gout.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatePostRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void init() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    private CreatePostRequest base() {
        CreatePostRequest r = new CreatePostRequest();
        r.setTitle("제목");
        r.setContent("내용");
        return r;
    }

    @Test
    @DisplayName("imageUrls=null 은 통과")
    void imageUrls_null_ok() {
        CreatePostRequest r = base();
        r.setImageUrls(null);
        assertEquals(0, violationCount(r, "imageUrlFormatValid"));
        assertEquals(0, violationCount(r, "imageUrlCountValid"));
    }

    @Test
    @DisplayName("imageUrls 빈 리스트는 통과")
    void imageUrls_empty_ok() {
        CreatePostRequest r = base();
        r.setImageUrls(List.of());
        assertEquals(0, violationCount(r, "imageUrlFormatValid"));
    }

    @Test
    @DisplayName("정상 업로드 경로 (32자 hex + png/jpg/webp) 통과")
    void imageUrls_valid_ok() {
        CreatePostRequest r = base();
        r.setImageUrls(List.of(
                "/api/uploads/posts/abcdef0123456789abcdef0123456789.png",
                "/api/uploads/posts/0123456789abcdef0123456789abcdef.jpg",
                "/api/uploads/posts/fedcba9876543210fedcba9876543210.webp"
        ));
        assertEquals(0, violationCount(r, "imageUrlFormatValid"));
    }

    @Test
    @DisplayName("외부 http(s) URL 차단 — Stored SSRF/Open Redirect 방지")
    void imageUrls_externalHttp_rejected() {
        CreatePostRequest r = base();
        r.setImageUrls(List.of("https://evil.example.com/malware.jpg"));
        assertTrue(violationCount(r, "imageUrlFormatValid") > 0);
    }

    @Test
    @DisplayName("file:// 스키마 차단")
    void imageUrls_fileScheme_rejected() {
        CreatePostRequest r = base();
        r.setImageUrls(List.of("file:///etc/passwd"));
        assertTrue(violationCount(r, "imageUrlFormatValid") > 0);
    }

    @Test
    @DisplayName("허용되지 않은 확장자 (gif) 차단")
    void imageUrls_disallowedExtension_rejected() {
        CreatePostRequest r = base();
        r.setImageUrls(List.of("/api/uploads/posts/abcdef0123456789abcdef0123456789.gif"));
        assertTrue(violationCount(r, "imageUrlFormatValid") > 0);
    }

    @Test
    @DisplayName("32자가 아닌 파일명 차단 (traversal/유추 방지)")
    void imageUrls_wrongFilenameLength_rejected() {
        CreatePostRequest r = base();
        r.setImageUrls(List.of("/api/uploads/posts/abc.png"));
        assertTrue(violationCount(r, "imageUrlFormatValid") > 0);
    }

    @Test
    @DisplayName("hex 가 아닌 문자 포함 파일명 차단")
    void imageUrls_nonHex_rejected() {
        CreatePostRequest r = base();
        r.setImageUrls(List.of("/api/uploads/posts/zzzzzzzz0123456789abcdef01234567.png"));
        assertTrue(violationCount(r, "imageUrlFormatValid") > 0);
    }

    @Test
    @DisplayName("../ path traversal 차단")
    void imageUrls_traversal_rejected() {
        CreatePostRequest r = base();
        r.setImageUrls(List.of("/api/uploads/posts/../../etc/passwd"));
        assertTrue(violationCount(r, "imageUrlFormatValid") > 0);
    }

    @Test
    @DisplayName("6장 이상 첨부 시 count 검증 위반")
    void imageUrls_tooMany_rejected() {
        CreatePostRequest r = base();
        String valid = "/api/uploads/posts/abcdef0123456789abcdef0123456789.png";
        r.setImageUrls(List.of(valid, valid, valid, valid, valid, valid));
        assertTrue(violationCount(r, "imageUrlCountValid") > 0);
    }

    @Test
    @DisplayName("null 엘리먼트 차단")
    void imageUrls_nullElement_rejected() {
        CreatePostRequest r = base();
        java.util.List<String> list = new java.util.ArrayList<>();
        list.add(null);
        r.setImageUrls(list);
        assertTrue(violationCount(r, "imageUrlFormatValid") > 0);
    }

    private int violationCount(CreatePostRequest r, String propertyFragment) {
        Set<ConstraintViolation<CreatePostRequest>> violations = validator.validate(r);
        return (int) violations.stream()
                .filter(v -> v.getPropertyPath().toString().contains(propertyFragment))
                .count();
    }
}
