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
package fr.cnes.regards.framework.modules.jobs.service.manager;

import java.util.concurrent.BlockingQueue;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import fr.cnes.regards.framework.modules.jobs.domain.IEvent;
import fr.cnes.regards.framework.modules.jobs.service.manager.JobHandler;
import fr.cnes.regards.framework.modules.jobs.service.manager.JobMonitor;

/**
 *
 */
public class JobMonitorTest {

    private JobMonitor jobMonitor;

    private JobHandler jobHandler;

    @Test
    public void testRunWhenException() throws InterruptedException {
        jobHandler = Mockito.mock(JobHandler.class);
        jobMonitor = new JobMonitor(jobHandler);
        final BlockingQueue<IEvent> queueMock = Mockito.mock(BlockingQueue.class);
        ReflectionTestUtils.setField(jobMonitor, "queueEvent", queueMock, BlockingQueue.class);

        Mockito.doThrow(new InterruptedException("some exception")).when(queueMock).take();

        jobMonitor.run();
        Mockito.verifyZeroInteractions(jobHandler);

    }
}
