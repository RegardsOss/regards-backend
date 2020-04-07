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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.ThreadLocalRandom;

import fr.cnes.regards.framework.random.function.FunctionDescriptor;

public class RandomLocalDateTime extends AbstractRandomGenerator<LocalDateTime> {

    private static final ZoneOffset REF_ZONE_OFFSET = ZoneOffset.UTC;

    private static final ZoneId REF_ZONE_ID = ZoneId.of("UTC");

    public RandomLocalDateTime(FunctionDescriptor fd) {
        super(fd);
    }

    @Override
    public LocalDateTime random() {
        return random(LocalDateTime.ofEpochSecond(0L, 0, REF_ZONE_OFFSET), LocalDateTime.now());
    }

    public LocalDateTime random(LocalDateTime startInclusive, LocalDateTime endExclusive) {
        LocalDateTime ldt = LocalDateTime
                .ofInstant(between(startInclusive.toInstant(REF_ZONE_OFFSET), endExclusive.toInstant(REF_ZONE_OFFSET)),
                           REF_ZONE_ID);
        return ldt;
    }

    private Instant between(Instant startInclusive, Instant endExclusive) {
        long startSeconds = startInclusive.getEpochSecond();
        long endSeconds = endExclusive.getEpochSecond();
        long random = ThreadLocalRandom.current().nextLong(startSeconds, endSeconds);

        return Instant.ofEpochSecond(random);
    }
}
