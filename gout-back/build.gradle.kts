plugins {
    java
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

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")   // JPA 기본 (Spring Data JPA)
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")   // /actuator/health 헬스체크

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

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
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
}
