/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.jobs.domain;

import java.net.URISyntaxException;
import java.time.OffsetDateTime;

import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 *
 */
public class StatusInfoTest {

    @Test
    public void testDomain() throws URISyntaxException {
        final StatusInfo statusInfo = new StatusInfo();
        final OffsetDateTime now = OffsetDateTime.now();
        final int percentCompleted = 48;
        final JobStatus status = JobStatus.RUNNING;
        final String description = "gjkdfhngjdfn";

        
        statusInfo.setEstimatedCompletion(now);
        
        OffsetDateTime startDate = now;
        startDate.withHour(15).minusDays(20);
        statusInfo.setStartDate(startDate);
        
        OffsetDateTime stopDate = now;
        startDate.withHour(0).minusDays(5);
        statusInfo.setStopDate(stopDate);
        
        statusInfo.setExpirationDate(now);
        statusInfo.setPercentCompleted(percentCompleted);
        statusInfo.setDescription(description);
        statusInfo.setJobStatus(status);

        Assertions.assertThat(statusInfo.getExpirationDate()).isEqualTo(now);
        Assertions.assertThat(statusInfo.getStartDate()).isEqualTo(startDate);
        Assertions.assertThat(statusInfo.getStopDate()).isEqualTo(stopDate);
        Assertions.assertThat(statusInfo.getEstimatedCompletion()).isEqualTo(now);
        Assertions.assertThat(statusInfo.getPercentCompleted()).isEqualTo(percentCompleted);
        Assertions.assertThat(statusInfo.getDescription()).isEqualTo(description);
        Assertions.assertThat(statusInfo.getJobStatus()).isEqualTo(status);

        JobStatus.valueOf(JobStatus.SUCCEEDED.toString());

    }
}
