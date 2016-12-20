/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.adapters;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    @Override
    public void write(JsonWriter pOut, LocalDateTime pValue) throws IOException {
        pOut.value(pValue.format(DateTimeFormatter.ISO_DATE_TIME));
    }

    @Override
    public LocalDateTime read(JsonReader pIn) throws IOException {
        return LocalDateTime.parse(pIn.nextString(), DateTimeFormatter.ISO_DATE_TIME);
    }
}
