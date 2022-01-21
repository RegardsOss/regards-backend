/* Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.utils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Varius utility functions regarding OffsetDateTime.
 *
 * @author gandrieu
 */
public class TimeUtils {

    public static final ZoneId UTC = ZoneId.of("UTC");

    private TimeUtils() {}

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
        return toUtc(date).toInstant().toEpochMilli();
    }

    public static OffsetDateTime toUtc(OffsetDateTime date) {
        return date.withOffsetSameInstant(ZoneOffset.UTC);
    }
}
