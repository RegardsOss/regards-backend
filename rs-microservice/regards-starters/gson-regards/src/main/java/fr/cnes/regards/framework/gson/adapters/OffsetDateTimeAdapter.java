/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.gson.adapters;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 *
 * ISO 8601 date adapter
 * This TypeAdapter is used in method GsonAutoConfiguration#customizeBuilder.
 * The aim is to be able to read a date time with or without Time zone specified and to format date time with UTC Time
 * Zone (ie. Z)
 * @author Marc Sordi
 * @author oroussel
 */
public class OffsetDateTimeAdapter extends TypeAdapter<OffsetDateTime> {

    /**
     * ISO date time official support (UTC)
     * When parsing, either no offset, Z or +HH:mm offset.
     * When formatting, Z as offset if UTC or +HH:mm
     */
    private static final DateTimeFormatter ISO_DATE_TIME_UTC = new DateTimeFormatterBuilder().parseCaseInsensitive()
            .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME).optionalStart().appendOffset("+HH:MM", "Z").toFormatter();

    /**
     * Writing date with UTC ISO 8601 format
     */
    @Override
    public void write(JsonWriter pOut, OffsetDateTime pValue) throws IOException {
        pOut.value(pValue.atZoneSameInstant(ZoneOffset.UTC).format(ISO_DATE_TIME_UTC));
    }

    @Override
    public OffsetDateTime read(JsonReader pIn) throws IOException {
        return parse(pIn.nextString());
    }

    public static OffsetDateTime parse(String date) {
        TemporalAccessor temporalAccessor = ISO_DATE_TIME_UTC.parse(date);
        // Zoned date
        if (temporalAccessor.isSupported(ChronoField.OFFSET_SECONDS)) {
            return OffsetDateTime.from(temporalAccessor);
        } else { // No zone specified => UTC date time
            return OffsetDateTime.of(LocalDateTime.from(temporalAccessor), ZoneOffset.UTC);
        }
    }

    public static String format(OffsetDateTime date) {
        return ISO_DATE_TIME_UTC.format(date.withOffsetSameInstant(ZoneOffset.UTC));
    }

}
