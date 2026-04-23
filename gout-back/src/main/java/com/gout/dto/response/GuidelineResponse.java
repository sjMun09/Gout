package com.gout.dto.response;

import com.gout.entity.Guideline;
import lombok.Getter;

import java.util.List;

@Getter
public class GuidelineResponse {

    private final String id;
    private final String type;
    private final String category;
    private final String title;
    private final String content;
    private final String evidenceStrength;
    private final String evidenceSource;
    private final String evidenceDoi;
    private final List<String> targetAgeGroups;

    private GuidelineResponse(String id,
                              String type,
                              String category,
                              String title,
                              String content,
                              String evidenceStrength,
                              String evidenceSource,
                              String evidenceDoi,
                              List<String> targetAgeGroups) {
        this.id = id;
        this.type = type;
        this.category = category;
        this.title = title;
        this.content = content;
        this.evidenceStrength = evidenceStrength;
        this.evidenceSource = evidenceSource;
        this.evidenceDoi = evidenceDoi;
        this.targetAgeGroups = targetAgeGroups;
    }

    public static GuidelineResponse of(Guideline g) {
        return new GuidelineResponse(
                g.getId(),
                g.getType() != null ? g.getType().name() : null,
                g.getCategory() != null ? g.getCategory().name() : null,
                g.getTitle(),
                g.getContent(),
                g.getEvidenceStrength() != null ? g.getEvidenceStrength().name() : null,
                g.getEvidenceSource(),
                g.getEvidenceDoi(),
                g.getTargetAgeGroups()
        );
    }
}
