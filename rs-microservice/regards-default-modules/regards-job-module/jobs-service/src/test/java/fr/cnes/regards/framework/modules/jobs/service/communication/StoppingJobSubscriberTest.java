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
package fr.cnes.regards.framework.modules.jobs.service.communication;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.domain.StatusInfo;
import fr.cnes.regards.framework.modules.jobs.service.communication.StoppingJobEvent;
import fr.cnes.regards.framework.modules.jobs.service.communication.StoppingJobSubscriber;
import fr.cnes.regards.framework.modules.jobs.service.manager.IJobHandler;

/**
 *
 */
public class StoppingJobSubscriberTest {

    private StoppingJobEvent stoppingJobEvent;

    private String projectName;

    private StoppingJobSubscriber newJobPuller;

    private IJobHandler jobHandlerMock;

    private long jobInfoIdExpected;

    @Before
    public void setUp() {
        jobHandlerMock = Mockito.mock(IJobHandler.class);
        newJobPuller = new StoppingJobSubscriber(jobHandlerMock);
        jobInfoIdExpected = 1L;
        stoppingJobEvent = new StoppingJobEvent(jobInfoIdExpected);
        projectName = "project1";
    }

    @Test
    public void testGetJob() {
        final StatusInfo statusInfo = new StatusInfo();
        statusInfo.setJobStatus(JobStatus.RUNNING);
        Mockito.when(jobHandlerMock.abort(jobInfoIdExpected)).thenReturn(statusInfo);
        final TenantWrapper<StoppingJobEvent> value = new TenantWrapper<>(stoppingJobEvent, projectName);
        newJobPuller.handle(value);
        Mockito.verify(jobHandlerMock).abort(jobInfoIdExpected);

    }

}
