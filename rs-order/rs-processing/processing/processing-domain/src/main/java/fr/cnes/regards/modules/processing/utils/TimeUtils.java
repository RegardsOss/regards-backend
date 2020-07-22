package fr.cnes.regards.modules.processing.utils;

import java.time.OffsetDateTime;
import java.time.ZoneId;

public class TimeUtils {

    public static OffsetDateTime nowUtc() {
        return OffsetDateTime.now(ZoneId.of("UTC"));
    }

}
