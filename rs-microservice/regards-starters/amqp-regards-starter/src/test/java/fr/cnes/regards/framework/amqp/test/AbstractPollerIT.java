/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.amqp.test;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.amqp.IPoller;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.VirtualHostMode;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.exception.RabbitMQVhostException;
import fr.cnes.regards.framework.amqp.test.event.PollableInfo;
import fr.cnes.regards.framework.amqp.test.event.PollableMicroserviceInfo;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 * Common poller tests for {@link VirtualHostMode#SINGLE} and {@link VirtualHostMode#MULTI} modes
 * @author Marc Sordi
 *
 */
public abstract class AbstractPollerIT {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPollerIT.class);

    @Autowired
    private IRabbitVirtualHostAdmin rabbitVirtualHostAdmin;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private IPoller poller;

    @Before
    public void init() throws RabbitMQVhostException {
        Assume.assumeTrue(rabbitVirtualHostAdmin.brokerRunning());
    }

    /**
     * Publish an event on a working queue. Get the event from the worker.
     */
    @Requirement("REGARDS_DSL_CMP_ARC_030")
    @Purpose("Event worker without restriction")
    @Test
    public void publishInfo() {
        String message = "Poll it!";
        PollableInfo info = new PollableInfo();
        info.setMessage(message);

        publisher.publish(info);

        // Simulate worker
        TenantWrapper<PollableInfo> wrapper = poller.poll(PollableInfo.class);
        Assert.assertNotNull(wrapper);
        Assert.assertEquals(PollableInfo.class, wrapper.getContent().getClass());
        Assert.assertEquals(message, wrapper.getContent().getMessage());

        wrapper = poller.poll(PollableInfo.class);
        Assert.assertNull(wrapper);
    }

    /**
     * Publish an event on a working queue. Get the event from the worker.
     */
    @Requirement("REGARDS_DSL_CMP_ARC_030")
    @Purpose("Event worker with restriction on microservice type")
    @Test
    public void publishMicroserviceInfo() {
        String message = "Poll it!";
        PollableMicroserviceInfo info = new PollableMicroserviceInfo();
        info.setMessage(message);

        publisher.publish(info);

        // Simulate worker
        TenantWrapper<PollableMicroserviceInfo> wrapper = poller.poll(PollableMicroserviceInfo.class);
        Assert.assertNotNull(wrapper);
        Assert.assertEquals(PollableMicroserviceInfo.class, wrapper.getContent().getClass());
        Assert.assertEquals(message, wrapper.getContent().getMessage());

        wrapper = poller.poll(PollableMicroserviceInfo.class);
        Assert.assertNull(wrapper);
    }
}
