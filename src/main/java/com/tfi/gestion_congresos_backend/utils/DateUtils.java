package com.tfi.gestion_congresos_backend.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;

public final class DateUtils {

    private static final ZoneId ARGENTINA_ZONE =
            ZoneId.of("America/Argentina/Buenos_Aires");

    private DateUtils() {
        // Evita instanciar la clase
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ARGENTINA_ZONE);
    }
}