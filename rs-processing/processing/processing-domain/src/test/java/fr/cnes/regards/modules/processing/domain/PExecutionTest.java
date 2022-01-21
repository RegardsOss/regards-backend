/* Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.domain;

import fr.cnes.regards.modules.processing.testutils.AbstractMarshallingTest;
import fr.cnes.regards.modules.processing.utils.gson.ProcessingGsonUtils;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import static fr.cnes.regards.modules.processing.domain.PStep.*;
import static fr.cnes.regards.modules.processing.utils.TimeUtils.nowUtc;

public class PExecutionTest extends AbstractMarshallingTest<PExecution> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PExecutionTest.class);

    @Test
    public void testJson() throws Exception {
        OffsetDateTime registered = nowUtc().minusMinutes(10);
        OffsetDateTime lastUpdate = nowUtc().minusMinutes(4);
        String json = ProcessingGsonUtils.gsonPretty().toJson(List.of(
            new PExecution(
                UUID.randomUUID(),
                "exec corr ID",
                UUID.randomUUID(),
                "batch corr ID",
                Duration.ofMinutes(5),
                List.of(new PInputFile(
                    "param1",
                    "file.raw",
                    null,
                    new URL("http://0.0.0.0:1000/file.raw"),
                    512L,
                    "checksum",
                    "file.raw",
                    HashMap.empty(),
                    "file.raw"
                )),
                List.of(registered("").withTime(registered),
                        prepare("").withTime(nowUtc().minusMinutes(5)),
                        running("").withTime(lastUpdate)
                ),
                "tenant",
                "user@regards.fr",
                UUID.randomUUID(),
                registered,
                lastUpdate,
                3,
                true
            )
        ));

        LOGGER.info("\n{}", json);
    }

    @Override public Class<PExecution> testedType() {
        return PExecution.class;
    }

}