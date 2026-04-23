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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "guidelines")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Guideline extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "guideline_type")
    private GuidelineType type;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "guideline_category")
    private GuidelineCategory category;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "evidence_strength", nullable = false, columnDefinition = "evidence_strength")
    private EvidenceStrength evidenceStrength;

    @Column(name = "evidence_source", length = 500)
    private String evidenceSource;

    @Column(name = "evidence_doi", length = 200)
    private String evidenceDoi;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "target_age_groups", columnDefinition = "text[]")
    private List<String> targetAgeGroups;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished;

    @Builder
    private Guideline(GuidelineType type,
                      GuidelineCategory category,
                      String title,
                      String content,
                      EvidenceStrength evidenceStrength,
                      String evidenceSource,
                      String evidenceDoi,
                      List<String> targetAgeGroups,
                      Boolean isPublished) {
        this.type = type;
        this.category = category;
        this.title = title;
        this.content = content;
        this.evidenceStrength = evidenceStrength != null ? evidenceStrength : EvidenceStrength.MODERATE;
        this.evidenceSource = evidenceSource;
        this.evidenceDoi = evidenceDoi;
        this.targetAgeGroups = targetAgeGroups;
        this.isPublished = isPublished != null ? isPublished : Boolean.TRUE;
    }

    public enum GuidelineType { DO, DONT }

    public enum GuidelineCategory { FOOD, EXERCISE, MEDICATION, LIFESTYLE, EMERGENCY }

    public enum EvidenceStrength { STRONG, MODERATE, WEAK }
}
