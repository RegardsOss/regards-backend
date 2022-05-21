/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

/**
 * ISO 8601 date adapter
 * This TypeAdapter is used in method GsonAutoConfiguration#customizeBuilder.
 * The aim is to be able to read a local date time
 *
 * @author Iliana Ghazali
 */
public class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {

    /**
     * ISO date time official support (UTC)
     */
    public static final DateTimeFormatter ISO_DATE_TIME_UTC = new DateTimeFormatterBuilder().parseCaseInsensitive()
                                                                                            .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                                                                            .toFormatter();

    /**
     * Writing date with UTC ISO 8601 format
     */
    @Override
    public void write(JsonWriter out, LocalDateTime value) throws IOException {
        out.value(value.format(ISO_DATE_TIME_UTC));
    }

    @Override
    public LocalDateTime read(JsonReader in) throws IOException {
        return parse(in.nextString());
    }

    public static LocalDateTime parse(String date) {
        try {
            return LocalDateTime.parse(date, ISO_DATE_TIME_UTC);
        } catch (DateTimeParseException e) {
            throw new JsonIOException("Date could not be parsed", e);
        }
    }

    public static String format(LocalDateTime date) {
        String formattedDate = null;
        if (date != null) {
            formattedDate = ISO_DATE_TIME_UTC.format(date);
        }
        return formattedDate;
    }

}
