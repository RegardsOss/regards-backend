package fr.cnes.regards.modules.processing.domain;

import fr.cnes.regards.modules.processing.domain.execution.ExecutionStatus;
import fr.cnes.regards.modules.processing.domain.parameters.ExecutionFileParameterValue;
import fr.cnes.regards.modules.processing.utils.GsonProcessingUtils;
import io.vavr.collection.List;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import static fr.cnes.regards.modules.processing.utils.TimeUtils.nowUtc;
import static org.junit.Assert.*;

public class PExecutionTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PExecutionTest.class);

    @Test
    public void testJson() throws Exception {
        OffsetDateTime registered = nowUtc().minusMinutes(10);
        OffsetDateTime lastUpdate = nowUtc().minusMinutes(4);
        String json = GsonProcessingUtils.gsonPretty().toJson(List.of(
            new PExecution(UUID.randomUUID(),
                           UUID.randomUUID(), Duration.ofMinutes(5),
                           List.of(new ExecutionFileParameterValue("param1", "file.raw", null, new URL("http://0.0.0.0:1000/file.raw"), 512L, "checksum", false)),
                           List.of(new PStep(ExecutionStatus.REGISTERED, registered, ""),
                                   new PStep(ExecutionStatus.PREPARE, nowUtc().minusMinutes(5), ""),
                                   new PStep(ExecutionStatus.RUNNING, lastUpdate, "")),
                           "tenant",
                           "user@regards.fr",
                           "nameOfTheProcess",
                           registered,
                           lastUpdate,
                           3,
                           true
            )
        ));

        LOGGER.info("\n{}", json);
    }

}