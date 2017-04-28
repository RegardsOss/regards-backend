/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.adapters;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 *
 * ISO 8601 date adapter
 * This TypeAdapter is used in method GsonAutoConfiguration#customizeBuilder.
 *
 * @author Marc Sordi
 */
public class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {

    /**
     * ISO date time official support (UTC)
     */
    private static final DateTimeFormatter ISO_DATE_TIME_UTC;
    static {
        ISO_DATE_TIME_UTC = new DateTimeFormatterBuilder().parseCaseInsensitive()
                .append(DateTimeFormatter.ISO_INSTANT).optionalStart().appendOffsetId().toFormatter();
    }

    @Override
    public void write(JsonWriter pOut, LocalDateTime pValue) throws IOException {
        pOut.value(pValue.format(ISO_DATE_TIME_UTC));
    }

    @Override
    public LocalDateTime read(JsonReader pIn) throws IOException {
        return LocalDateTime.parse(pIn.nextString(), ISO_DATE_TIME_UTC);
    }

    public static LocalDateTime parse(String date) {
        return LocalDateTime.from(ISO_DATE_TIME_UTC.parse(date));
    }

    public static String format(LocalDateTime date) {
        return ISO_DATE_TIME_UTC.format(date);
    }

}
