package com.gout.dto.response;

import com.gout.entity.Notification;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponse {

    private final String id;
    private final String type;
    private final String title;
    private final String body;
    private final String link;
    private final boolean read;
    private final LocalDateTime readAt;
    private final LocalDateTime createdAt;

    public static NotificationResponse of(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .body(n.getBody())
                .link(n.getLink())
                .read(n.isRead())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
