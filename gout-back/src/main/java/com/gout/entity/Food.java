package com.gout.entity;

import com.gout.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Table(name = "foods")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Food extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "name_en")
    private String nameEn;

    private String category;

    @Column(name = "purine_content")
    private BigDecimal purineContent;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "purine_level", nullable = false, columnDefinition = "purine_level")
    private PurineLevel purineLevel;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "food_recommendation")
    private FoodRecommendation recommendation;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String caution;

    @Column(name = "evidence_notes", columnDefinition = "TEXT")
    private String evidenceNotes;

    // 시드 데이터로만 들어오므로 빌더 생략. 관리자 업데이트용 메서드만 제공.
    public void update(String name,
                       String nameEn,
                       String category,
                       BigDecimal purineContent,
                       PurineLevel purineLevel,
                       FoodRecommendation recommendation,
                       String description,
                       String caution,
                       String evidenceNotes) {
        this.name = name;
        this.nameEn = nameEn;
        this.category = category;
        this.purineContent = purineContent;
        this.purineLevel = purineLevel;
        this.recommendation = recommendation;
        this.description = description;
        this.caution = caution;
        this.evidenceNotes = evidenceNotes;
    }

    public enum PurineLevel { LOW, MEDIUM, HIGH, VERY_HIGH }

    public enum FoodRecommendation { GOOD, MODERATE, BAD, AVOID }
}
