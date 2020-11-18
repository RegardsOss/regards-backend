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
                "nameOfTheProcess",
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