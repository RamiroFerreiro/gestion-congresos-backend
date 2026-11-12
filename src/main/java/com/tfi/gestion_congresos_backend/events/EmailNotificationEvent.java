package com.tfi.gestion_congresos_backend.events;

public record EmailNotificationEvent(
        String recipient,
        String subject,
        String content,
        ContentType contentType
) {

    public enum ContentType {
        TEXT,
        HTML
    }
}
