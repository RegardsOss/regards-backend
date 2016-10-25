/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.domain;

import java.net.URISyntaxException;
import java.time.LocalDateTime;

import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 *
 */
public class StatusInfoTest {

    @Test
    public void testDomain() throws URISyntaxException {
        final StatusInfo statusInfo = new StatusInfo();
        final LocalDateTime now = LocalDateTime.now();
        final int percentCompleted = 48;
        final JobStatus status = JobStatus.RUNNING;
        final String description = "gjkdfhngjdfn";

        statusInfo.setEstimatedCompletion(now);
        statusInfo.setStartDate(now);
        statusInfo.setStopDate(now);
        statusInfo.setExpirationDate(now);
        statusInfo.setPercentCompleted(percentCompleted);
        statusInfo.setDescription(description);
        statusInfo.setJobStatus(status);

        Assertions.assertThat(statusInfo.getExpirationDate()).isEqualTo(now);
        Assertions.assertThat(statusInfo.getStartDate()).isEqualTo(now);
        Assertions.assertThat(statusInfo.getStopDate()).isEqualTo(now);
        Assertions.assertThat(statusInfo.getEstimatedCompletion()).isEqualTo(now);
        Assertions.assertThat(statusInfo.getPercentCompleted()).isEqualTo(percentCompleted);
        Assertions.assertThat(statusInfo.getDescription()).isEqualTo(description);
        Assertions.assertThat(statusInfo.getJobStatus()).isEqualTo(status);

        JobStatus.valueOf(JobStatus.SUCCEEDED.toString());

    }
}
