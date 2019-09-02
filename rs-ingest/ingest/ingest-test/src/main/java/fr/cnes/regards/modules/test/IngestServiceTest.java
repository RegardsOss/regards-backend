/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.test;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.configuration.AmqpConstants;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.modules.ingest.client.IngestRequestEventHandler;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.IIngestRequestRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.dto.sip.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.dto.sip.flow.IngestRequestFlowItem;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IngestServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestServiceTest.class);

    @Autowired
    protected IIngestRequestRepository ingestRequestRepository;

    @Autowired
    protected ISIPRepository sipRepository;

    @Autowired
    protected IAIPRepository aipRepository;

    @Autowired
    private IJobInfoRepository jobInfoRepo;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private IPluginConfigurationRepository pluginConfRepo;

    @Autowired(required = false)
    private IAmqpAdmin amqpAdmin;

    @Autowired(required = false)
    private IRabbitVirtualHostAdmin vhostAdmin;

    /**
     * Clean everything a test can use, to prepare the empty environment for the next test
     * @throws Exception
     */
    public void init() throws Exception {
        aipRepository.deleteAll();
        sipRepository.deleteAll();
        ingestRequestRepository.deleteAll();
        jobInfoRepo.deleteAll();
        pluginConfRepo.deleteAll();
        cleanAMQPQueues();
    }

    /**
     * Internal method to clean AMQP queues, if actives
     */
    private void cleanAMQPQueues() {
        if (vhostAdmin != null) {
            // Re-set tenant because above simulation clear it!

            // Purge event queue
            try {
                vhostAdmin.bind(AmqpConstants.AMQP_MULTITENANT_MANAGER);
                amqpAdmin.purgeQueue(amqpAdmin.getSubscriptionQueueName(IngestRequestEventHandler.class,
                        Target.ONE_PER_MICROSERVICE_TYPE),
                        false);
            } finally {
                vhostAdmin.unbind();
            }
        }
    }


    public void waitForIngestion(long expectedSips) {
        waitForIngestion(expectedSips, expectedSips * 1000);
    }
        /**
         * Helper method to wait for SIP ingestion
         * @param expectedSips expected count of sips in database
         * @param timeout in ms
         * @throws InterruptedException
         */
    public void waitForIngestion(long expectedSips, long timeout) {

        long end = System.currentTimeMillis() + timeout;
        // Wait
        long sipCount;
        do {
            sipCount = sipRepository.count();
            LOGGER.debug("{} SIP(s) created in database", sipCount);
            if (sipCount == expectedSips) {
                break;
            }
            long now = System.currentTimeMillis();
            if (end > now) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Assert.fail("Thread interrupted");
                }
            } else {
                Assert.fail("Timeout");
            }
        } while (true);
    }

    public void waitDuring(long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Assert.fail("Wait interrupted");
        }
    }

    /**
     * Send the event to ingest a new SIP
     * @param sip
     * @param mtd
     */
    public void sendIngestRequestEvent(SIP sip, IngestMetadataDto mtd) {
        IngestRequestFlowItem flowItem = IngestRequestFlowItem.build(mtd, sip);
        publisher.publish(flowItem);
    }
}
