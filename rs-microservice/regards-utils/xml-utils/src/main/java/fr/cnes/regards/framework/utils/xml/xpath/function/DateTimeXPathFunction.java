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
package fr.cnes.regards.framework.utils.xml.xpath.function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.List;

public class DateTimeXPathFunction implements NamedXPathFunction {

    public static final DateTimeFormatter ISO_DATE_TIME_UTC = new DateTimeFormatterBuilder().parseCaseInsensitive()
                                                                                            .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                                                                            .optionalStart()
                                                                                            .appendOffset("+HH:MM", "Z")
                                                                                            .toFormatter();

    private static final Logger LOGGER = LoggerFactory.getLogger(DateTimeXPathFunction.class);

    @Override
    public String getFunctionName() {
        return "datetime";
    }

    @Override
    public Object evaluate(@SuppressWarnings("rawtypes") List args) {
        if (args.size() == 1) {
            return parse(args, ISO_DATE_TIME_UTC);
        } else {
            return parse(args, DateTimeFormatter.ofPattern((String) args.get(1)));
        }
    }

    private NodeList parse(@SuppressWarnings("rawtypes") List args, DateTimeFormatter formatter) {

        NodeList nodes = (NodeList) args.get(0);
        for (int index = 0; index < nodes.getLength(); index++) {
            OffsetDateTime offsetDateTime = parse(nodes.item(index).getTextContent(), formatter);
            if (offsetDateTime != null) {
                nodes.item(index).setTextContent(offsetDateTime.toString());
            }
        }
        return nodes;
    }

    private OffsetDateTime parse(String datetime, DateTimeFormatter formatter) {
        try {
            TemporalAccessor temporalAccessor = formatter.parse(datetime);
            // Zoned date
            if (temporalAccessor.isSupported(ChronoField.OFFSET_SECONDS)) {
                return OffsetDateTime.from(temporalAccessor);
            } else { // No zone specified => UTC date time
                return OffsetDateTime.of(LocalDateTime.from(temporalAccessor), ZoneOffset.UTC);
            }
        } catch (DateTimeParseException e) {
            LOGGER.error(String.format("Failure in function %s with args %s", getFunctionName(), datetime), e);
        }
        return null;
    }

}
