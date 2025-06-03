/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.gson.JsonIOException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import fr.cnes.regards.framework.utils.json.JsonSerializationUtils;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * ISO 8601 date adapter
 * This TypeAdapter is used in method GsonAutoConfiguration#customizeBuilder.
 * The aim is to be able to read a date time with or without Time zone specified and to format date time with UTC Time
 * Zone (ie. Z)
 *
 * @author Marc Sordi
 * @author oroussel
 */
public class OffsetDateTimeAdapter extends TypeAdapter<OffsetDateTime> {

    /**
     * Writing date with UTC ISO 8601 format
     */
    @Override
    public void write(JsonWriter out, OffsetDateTime date) throws IOException {
        // truncate to a resolution of 1 microsecond
        out.value(JsonSerializationUtils.format(date.atZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS)));
    }

    @Override
    public OffsetDateTime read(JsonReader in) throws IOException {
        return parse(in.nextString());
    }

    public static OffsetDateTime parse(String date) {
        try {
            return JsonSerializationUtils.parse(date);
        } catch (DateTimeParseException e) {
            throw new JsonIOException("Date could not be parsed", e);
        }
    }

    public static String format(OffsetDateTime date) {
        return JsonSerializationUtils.format(date);
    }

}
