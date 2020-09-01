package fr.cnes.regards.modules.processing.utils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public class TimeUtils {

    public static final ZoneId UTC = ZoneId.of("UTC");

    public static OffsetDateTime parseUtc(String repr) {
        return OffsetDateTime.ofInstant(Instant.parse(repr), ZoneId.of("UTC"));
    }

    public static OffsetDateTime nowUtc() {
        return OffsetDateTime.now(UTC);
    }

    public static OffsetDateTime fromEpochMillisUTC(Long millis) {
        return Instant.ofEpochMilli(millis).atZone(UTC).toOffsetDateTime();
    }

    public static long toEpochMillisUTC(OffsetDateTime date) {
        return date.atZoneSameInstant(UTC).toInstant().toEpochMilli();
    }

}
