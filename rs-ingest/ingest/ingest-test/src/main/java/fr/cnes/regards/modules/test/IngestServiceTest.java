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

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpIOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.configuration.AmqpConstants;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.IAIPUpdateRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IAbstractRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IIngestProcessingChainRepository;
import fr.cnes.regards.modules.ingest.dao.IIngestRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IOAISDeletionRequestRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.dao.IStorageDeletionRequestRepository;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.request.event.IngestRequestEvent;
import fr.cnes.regards.modules.ingest.dto.sip.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.dto.sip.flow.IngestRequestFlowItem;
import fr.cnes.regards.modules.storage.client.FileRequestGroupEventHandler;
import fr.cnes.regards.modules.storage.domain.event.FileRequestsGroupEvent;

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
    protected IAbstractRequestRepository requestRepository;

    @Autowired
    private IJobInfoRepository jobInfoRepo;

    @Autowired
    private IStorageDeletionRequestRepository storageDeletionRequestRepository;

    @Autowired
    private IOAISDeletionRequestRepository deletionRequestRepository;

    @Autowired
    private IAIPUpdateRequestRepository aipUpdateRequestRepository;

    @Autowired
    private IAbstractRequestRepository abstractRequestRepository;

    @Autowired
    private IIngestProcessingChainRepository ingestProcessingChainRepository;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private IPluginConfigurationRepository pluginConfRepo;

    @Autowired(required = false)
    private IAmqpAdmin amqpAdmin;

    @Autowired(required = false)
    private IRabbitVirtualHostAdmin vhostAdmin;

    @Autowired
    private ISubscriber subscriber;

    /**
     * Clean everything a test can use, to prepare the empty environment for the next test
     * @throws Exception
     */
    public void init() throws Exception {
        ingestProcessingChainRepository.deleteAllInBatch();
        ingestRequestRepository.deleteAllInBatch();
        requestRepository.deleteAllInBatch();
        aipRepository.deleteAllInBatch();
        sipRepository.deleteAllInBatch();
        jobInfoRepo.deleteAll();
        pluginConfRepo.deleteAllInBatch();
        cleanAMQPQueues(FileRequestGroupEventHandler.class, Target.ONE_PER_MICROSERVICE_TYPE);
    }

    public void clear() {
        // WARNING : clean context manually because Spring doesn't do it between tests
        subscriber.unsubscribeFrom(IngestRequestFlowItem.class);
        subscriber.unsubscribeFrom(IngestRequestEvent.class);
        subscriber.unsubscribeFrom(FileRequestsGroupEvent.class);
    }

    /**
     * Internal method to clean AMQP queues, if actives
     */
    public void cleanAMQPQueues(Class<? extends IHandler<?>> handler, Target target) {
        if (vhostAdmin != null) {
            // Re-set tenant because above simulation clear it!

            // Purge event queue
            try {
                vhostAdmin.bind(AmqpConstants.AMQP_MULTITENANT_MANAGER);
                amqpAdmin.purgeQueue(amqpAdmin.getSubscriptionQueueName(handler, target), false);
            } catch (AmqpIOException e) {
                //todo
            } finally {
                vhostAdmin.unbind();
            }
        }
    }

    public void waitForIngestion(long expectedSips) {
        waitForIngestion(expectedSips, expectedSips * 1000);
    }

    public void waitForIngestion(long expectedSips, long timeout) {
        waitForIngestion(expectedSips, timeout, null);
    }

    /**
     * Helper method to wait for SIP ingestion
     * @param expectedSips expected count of sips in database
     * @param timeout in ms
     * @throws InterruptedException
     */
    public void waitForIngestion(long expectedSips, long timeout, SIPState sipState) {
        long end = System.currentTimeMillis() + timeout;
        // Wait
        long sipCount;
        do {
            if (sipState != null) {
                sipCount = sipRepository.countByState(sipState);
            } else {
                sipCount = sipRepository.count();
            }
            LOGGER.debug("{} SIP(s) created in database", sipCount);
            if (sipCount >= expectedSips) {
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

    /**
     * Helper method that waits all requests have been processed
     * @param timeout
     */
    public void waitAllRequestsFinished(long timeout) {
        long end = System.currentTimeMillis() + timeout;
        // Wait
        do {
            long count = abstractRequestRepository.count();
            LOGGER.debug("{} Current request running", count);
            if (count == 0) {
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
