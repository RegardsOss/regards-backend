/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.domain;

import java.util.ArrayList;

import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 *
 */
public class EventTest {

    @Test
    public void testDomain() {
        final long jobInfoId = 5L;
        final ArrayList<Object> data = new ArrayList<>();
        final EventType eventType = EventType.RUN_ERROR;
        final String tenantName = "project1";
        final Event event1 = new Event(eventType, data, jobInfoId, tenantName);

        Assertions.assertThat(event1.getJobInfoId()).isEqualTo(jobInfoId);
        Assertions.assertThat(event1.getData()).isEqualTo(data);
        Assertions.assertThat(event1.getType()).isEqualTo(eventType);
        EventType.valueOf(EventType.RUN_ERROR.toString());

    }
}
