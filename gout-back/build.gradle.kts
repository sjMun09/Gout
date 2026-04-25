plugins {
    java
    jacoco
    id("org.springframework.boot") version "4.0.5"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.gout"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

val querydslVersion = "5.1.0"
val jjwtVersion = "0.13.0"
val testcontainersVersion = "1.20.4"
val springdocVersion = "3.0.3"  // Spring Boot 4.0.5 대응 — parent pom 명시 호환

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")   // JPA 기본 (Spring Data JPA)
    implementation("org.springframework.boot:spring-boot-starter-data-redis") // 리프레시 토큰 저장소 (P1-8)
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")   // /actuator/health 헬스체크
    implementation("org.springframework.boot:spring-boot-starter-jdbc")       // JdbcTemplate (pgvector native UPDATE)

    // Jackson XML (PubMed efetch XML 파싱용)
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")

    // PostgreSQL
    runtimeOnly("org.postgresql:postgresql")

    // Flyway
    // Spring Boot 4.x 부터 Flyway auto-configuration 이 별도 모듈(spring-boot-flyway) 로 분리됨.
    // 이 의존성이 없으면 flyway-core 는 포함되지만 auto-config 가 안 돌아 마이그레이션이 실행되지 않는다.
    implementation("org.springframework.boot:spring-boot-flyway")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // QueryDSL — 복잡한 동적 쿼리가 필요한 도메인에만 선택적으로 적용
    implementation("com.querydsl:querydsl-jpa:$querydslVersion:jakarta")
    annotationProcessor("com.querydsl:querydsl-apt:$querydslVersion:jakarta")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api:3.2.0")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api:2.1.1")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    // Springdoc OpenAPI (Swagger UI)
    // - /v3/api-docs     JSON
    // - /swagger-ui.html 리다이렉트 → /swagger-ui/index.html
    // SecurityConfig 는 Agent-I 가 수정중이므로 여기선 건드리지 않고,
    // OpenApiWebSecurityCustomizer 빈으로 /swagger-ui/**, /v3/api-docs/** 만 permitAll 추가.
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")

    // Bucket4j — 로그인/회원가입/좋아요 레이트 리밋.
    // 멀티 인스턴스 간 버킷 공유를 위해 Lettuce-backed Redis 프록시 사용 (HIGH-004).
    // Spring Data Redis(refresh token store, P1-8) 가 이미 있어 Redis 인프라는 재사용.
    implementation("com.bucket4j:bucket4j_jdk17-core:8.14.0")
    implementation("com.bucket4j:bucket4j_jdk17-lettuce:8.14.0")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// QueryDSL Q-type 생성 경로 (annotationProcessor가 Q클래스 생성하는 위치)
val querydslDir = layout.buildDirectory.dir("generated/querydsl")

sourceSets {
    main {
        java {
            srcDir(querydslDir)
        }
    }
}

tasks.withType<JavaCompile> {
    options.generatedSourceOutputDirectory = querydslDir.get().asFile
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

// JaCoCo — `./gradlew test jacocoTestReport` 후
//   build/reports/jacoco/test/html/index.html 생성.
// 커버리지 임계치는 아직 강제하지 않음 (테스트 다수가 @Disabled 상태).
jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        html.required.set(true)
        xml.required.set(true)  // CI 업로드용
        csv.required.set(false)
    }
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                // 수동 작성 코드만 커버리지 대상으로. 자동 생성/DTO/Q-type 제외.
                exclude(
                    "**/Q*.class",                    // QueryDSL Q-type
                    "com/gout/dto/**",                // DTO
                    "com/gout/GoutApplication.class", // main
                    "com/gout/global/entity/**",      // BaseEntity
                    "**/config/**"                    // Config (대부분 빈 설정)
                )
            }
        })
    )
}
