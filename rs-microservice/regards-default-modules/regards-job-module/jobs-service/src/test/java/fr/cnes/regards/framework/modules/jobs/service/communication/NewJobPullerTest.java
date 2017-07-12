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

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.cnes.regards.framework.amqp.Poller;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;

/**
 *
 */
public class NewJobPullerTest {

    private INewJobPuller newJobPullerMessageBroker;

    private Poller pollerMock;

    private NewJobEvent newJobEvent;

    private String projectName;

    @Before
    public void setUp() {
        pollerMock = Mockito.mock(Poller.class);
        pollerMock.toString();
        newJobPullerMessageBroker = new NewJobPuller(pollerMock);
        newJobEvent = new NewJobEvent(1L);
        projectName = "project1";
    }

    @Test
    public void testGetJob() throws RabbitMQVhostException {
        final long jobInfoIdExpected = 666L;
        // Also test the setter
        newJobEvent.setJobInfoId(jobInfoIdExpected);
        final TenantWrapper<NewJobEvent> value = new TenantWrapper<>(newJobEvent, projectName);
        Mockito.when(pollerMock.poll(NewJobEvent.class)).thenReturn(value);
        final Long jobInfoId = newJobPullerMessageBroker.getJob(projectName);
        Assertions.assertThat(jobInfoId).isEqualTo(jobInfoIdExpected);
    }

    @Test
    public void testGetJobWhenRabbitException() throws RabbitMQVhostException {
        Mockito.doThrow(new RabbitMQVhostException("some exception")).when(pollerMock).poll(newJobEvent.getClass());

        final Long jobInfoId = newJobPullerMessageBroker.getJob(projectName);
        Assertions.assertThat(jobInfoId).isNull();

    }
}
