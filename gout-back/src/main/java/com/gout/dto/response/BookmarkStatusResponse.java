package com.gout.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BookmarkStatusResponse {

    private final boolean bookmarked;

    public static BookmarkStatusResponse of(boolean bookmarked) {
        return new BookmarkStatusResponse(bookmarked);
    }
}
