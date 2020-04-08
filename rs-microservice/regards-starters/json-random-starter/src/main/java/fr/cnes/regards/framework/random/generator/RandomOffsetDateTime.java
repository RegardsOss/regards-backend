/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.random.generator;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.concurrent.ThreadLocalRandom;

import fr.cnes.regards.framework.random.function.FunctionDescriptor;

public class RandomOffsetDateTime extends AbstractRandomGenerator<OffsetDateTime> {

    private static String USAGE = "Function {} only support 0 or 2 arguments (see DateTimeFormatter.ISO_LOCAL_DATE)";

    private static final ZoneOffset REF_ZONE_OFFSET = ZoneOffset.UTC;

    private static final ZoneId REF_ZONE_ID = ZoneId.of("UTC");

    private static final String EXPANDED_DATE = "T00:00:00";

    private OffsetDateTime startInclusive;

    private OffsetDateTime endExclusive;

    public RandomOffsetDateTime(FunctionDescriptor fd) {
        super(fd);
    }

    @Override
    public void parseParameters() {
        switch (fd.getParameterSize()) {
            case 0:
                break;
            case 2:
                TemporalAccessor ta1 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.parse(fd.getParameter(0) + EXPANDED_DATE);
                TemporalAccessor ta2 = DateTimeFormatter.ISO_LOCAL_DATE_TIME.parse(fd.getParameter(1) + EXPANDED_DATE);
                startInclusive = OffsetDateTime.of(LocalDateTime.from(ta1), REF_ZONE_OFFSET);
                endExclusive = OffsetDateTime.of(LocalDateTime.from(ta2), REF_ZONE_OFFSET);
                break;
            default:
                throw new IllegalArgumentException(String.format(USAGE, fd.getType()));
        }
    }

    @Override
    public OffsetDateTime random() {
        switch (fd.getParameterSize()) {
            case 0:
                return random(OffsetDateTime.ofInstant(Instant.EPOCH, REF_ZONE_ID), OffsetDateTime.now());
            case 2:
                return random(startInclusive, endExclusive);
            default:
                throw new IllegalArgumentException(String.format(USAGE, fd.getType()));
        }
    }

    public OffsetDateTime random(OffsetDateTime startInclusive, OffsetDateTime endExclusive) {
        OffsetDateTime odt = OffsetDateTime.ofInstant(between(startInclusive.toInstant(), endExclusive.toInstant()),
                                                      REF_ZONE_ID);
        return odt;
    }

    private Instant between(Instant startInclusive, Instant endExclusive) {
        long startSeconds = startInclusive.getEpochSecond();
        long endSeconds = endExclusive.getEpochSecond();
        long random = ThreadLocalRandom.current().nextLong(startSeconds, endSeconds);

        return Instant.ofEpochSecond(random);
    }
}
