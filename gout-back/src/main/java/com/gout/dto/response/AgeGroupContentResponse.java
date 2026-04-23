package com.gout.dto.response;

import com.gout.entity.AgeGroupContent;
import lombok.Getter;

@Getter
public class AgeGroupContentResponse {

    private final String id;
    private final String ageGroup;
    private final String title;
    private final String characteristics;
    private final String mainCauses;
    private final String warnings;
    private final String managementTips;
    private final String evidenceSource;

    private AgeGroupContentResponse(String id,
                                    String ageGroup,
                                    String title,
                                    String characteristics,
                                    String mainCauses,
                                    String warnings,
                                    String managementTips,
                                    String evidenceSource) {
        this.id = id;
        this.ageGroup = ageGroup;
        this.title = title;
        this.characteristics = characteristics;
        this.mainCauses = mainCauses;
        this.warnings = warnings;
        this.managementTips = managementTips;
        this.evidenceSource = evidenceSource;
    }

    public static AgeGroupContentResponse of(AgeGroupContent c) {
        return new AgeGroupContentResponse(
                c.getId(),
                c.getAgeGroup() != null ? c.getAgeGroup().name() : null,
                c.getTitle(),
                c.getCharacteristics(),
                c.getMainCauses(),
                c.getWarnings(),
                c.getManagementTips(),
                c.getEvidenceSource()
        );
    }
}
