package com.gout.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FoodSearchRequest {

    private String keyword;

    /** LOW | MEDIUM | HIGH | VERY_HIGH */
    private String purineLevel;

    private String category;

    private int page = 0;

    private int size = 20;
}
