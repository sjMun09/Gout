package com.gout.config;

import com.gout.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Issue #88 — OpenAPI 계약 통합 테스트.
 *
 * <p>{@code GET /v3/api-docs} 응답을 파싱해 다음 항목을 검증:
 * <ul>
 *   <li>표준 에러 응답 컴포넌트({@code ErrorResponse})가 등록되어 있는가</li>
 *   <li>{@code components.securitySchemes.bearerAuth} 가 HTTP/Bearer 로 정의되어 있는가</li>
 *   <li>주요 태그(Auth/User/Post/Health/Admin)가 등록되어 있는가</li>
 *   <li>주요 엔드포인트가 기대하는 상태 코드(401/403/404 등)를 노출하는가</li>
 * </ul>
 *
 * <p>{@link IntegrationTestBase} 가 Testcontainers(Postgres+pgvector / Redis) 를 띄우므로
 * 로컬 환경에 Docker 와 사전 빌드된 이미지({@code gout-test-postgis-pgvector:local}) 가 필요하다.
 * Docker 미설치 환경에서는 컨테이너 부팅 단계에서 실패하지만, CI 에서는 정상 실행된다.
 */
class OpenApiContractIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("/v3/api-docs 가 표준 ErrorResponse / bearerAuth / 핵심 태그·상태코드를 노출한다")
    void documents_standard_error_responses_and_tags() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());

        // 1) ErrorResponse 컴포넌트가 등록되어 있고 핵심 필드를 가진다.
        JsonNode schemas = root.path("components").path("schemas");
        assertThat(schemas.has("ErrorResponse"))
                .as("components.schemas.ErrorResponse should exist")
                .isTrue();
        JsonNode errorSchemaProps = schemas.path("ErrorResponse").path("properties");
        for (String field : new String[]{"success", "code", "message", "status", "path", "timestamp"}) {
            assertThat(errorSchemaProps.has(field))
                    .as("ErrorResponse.properties.%s", field)
                    .isTrue();
        }

        // 2) bearerAuth security scheme.
        JsonNode bearer = root.path("components").path("securitySchemes").path("bearerAuth");
        assertThat(bearer.path("type").asText()).isEqualTo("http");
        assertThat(bearer.path("scheme").asText()).isEqualTo("bearer");
        assertThat(bearer.path("bearerFormat").asText()).isEqualTo("JWT");

        // 3) 주요 태그가 등록되어 있다.
        JsonNode tags = root.path("tags");
        assertThat(tags.isArray()).isTrue();
        java.util.List<String> tagNames = new java.util.ArrayList<>();
        tags.forEach(t -> tagNames.add(t.path("name").asText()));
        assertThat(tagNames).contains("Auth", "User", "Post", "Health", "Admin");

        // 4) 주요 엔드포인트의 상태 코드.
        JsonNode paths = root.path("paths");

        // /api/auth/login: 200 / 401 / 400 / 422
        JsonNode loginResponses = paths.path("/api/auth/login").path("post").path("responses");
        for (String code : new String[]{"200", "401", "400", "422"}) {
            assertThat(loginResponses.has(code))
                    .as("POST /api/auth/login responses.%s", code)
                    .isTrue();
        }

        // /api/me GET: 200 / 401
        JsonNode meResponses = paths.path("/api/me").path("get").path("responses");
        for (String code : new String[]{"200", "401"}) {
            assertThat(meResponses.has(code))
                    .as("GET /api/me responses.%s", code)
                    .isTrue();
        }

        // /api/posts/{id} GET: 200 / 404
        JsonNode postDetailResponses = paths.path("/api/posts/{id}").path("get").path("responses");
        for (String code : new String[]{"200", "404"}) {
            assertThat(postDetailResponses.has(code))
                    .as("GET /api/posts/{id} responses.%s", code)
                    .isTrue();
        }

        // /api/admin/users GET: 200 / 403
        JsonNode adminListResponses = paths.path("/api/admin/users").path("get").path("responses");
        for (String code : new String[]{"200", "403"}) {
            assertThat(adminListResponses.has(code))
                    .as("GET /api/admin/users responses.%s", code)
                    .isTrue();
        }
    }
}
