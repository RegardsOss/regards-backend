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
 *
 * @author Marc Sordi
 *
 */
public class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {

    /**
     * ISO date time official support
     */
    public static final DateTimeFormatter ISO_DATE_TIME_OPTIONAL_OFFSET;
    static {
        ISO_DATE_TIME_OPTIONAL_OFFSET = new DateTimeFormatterBuilder().parseCaseInsensitive()
                .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME).optionalStart().appendOffsetId().toFormatter();
    }

    @Override
    public void write(JsonWriter pOut, LocalDateTime pValue) throws IOException {
        pOut.value(pValue.format(ISO_DATE_TIME_OPTIONAL_OFFSET));
    }

    @Override
    public LocalDateTime read(JsonReader pIn) throws IOException {
        return LocalDateTime.parse(pIn.nextString(), ISO_DATE_TIME_OPTIONAL_OFFSET);
    }
}
