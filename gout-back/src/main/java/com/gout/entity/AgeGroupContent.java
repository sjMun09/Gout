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

@Entity
@Table(name = "age_group_contents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgeGroupContent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "age_group", nullable = false, columnDefinition = "age_group")
    private AgeGroup ageGroup;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String characteristics;

    @Column(name = "main_causes", nullable = false, columnDefinition = "TEXT")
    private String mainCauses;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String warnings;

    @Column(name = "management_tips", nullable = false, columnDefinition = "TEXT")
    private String managementTips;

    @Column(name = "evidence_source", length = 500)
    private String evidenceSource;

    @Column(name = "evidence_doi", length = 200)
    private String evidenceDoi;

    @Builder
    private AgeGroupContent(AgeGroup ageGroup,
                            String title,
                            String characteristics,
                            String mainCauses,
                            String warnings,
                            String managementTips,
                            String evidenceSource,
                            String evidenceDoi) {
        this.ageGroup = ageGroup;
        this.title = title;
        this.characteristics = characteristics;
        this.mainCauses = mainCauses;
        this.warnings = warnings;
        this.managementTips = managementTips;
        this.evidenceSource = evidenceSource;
        this.evidenceDoi = evidenceDoi;
    }

    public enum AgeGroup { TWENTIES, THIRTIES, FORTIES, FIFTIES, SIXTIES, SEVENTIES_PLUS }
}
