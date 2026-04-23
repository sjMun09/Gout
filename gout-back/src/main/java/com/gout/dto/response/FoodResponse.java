package com.gout.dto.response;

import com.gout.entity.Food;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class FoodResponse {

    private final String id;
    private final String name;
    private final String nameEn;
    private final String category;
    private final BigDecimal purineContent;
    private final String purineLevel;
    private final String recommendation;
    private final String description;
    private final String caution;
    private final String evidenceNotes;

    private FoodResponse(String id,
                         String name,
                         String nameEn,
                         String category,
                         BigDecimal purineContent,
                         String purineLevel,
                         String recommendation,
                         String description,
                         String caution,
                         String evidenceNotes) {
        this.id = id;
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

    public static FoodResponse of(Food f) {
        return new FoodResponse(
                f.getId(),
                f.getName(),
                f.getNameEn(),
                f.getCategory(),
                f.getPurineContent(),
                f.getPurineLevel() != null ? f.getPurineLevel().name() : null,
                f.getRecommendation() != null ? f.getRecommendation().name() : null,
                f.getDescription(),
                f.getCaution(),
                f.getEvidenceNotes()
        );
    }
}
