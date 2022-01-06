/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.IAbstractRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IIngestProcessingChainRepository;
import fr.cnes.regards.modules.ingest.dao.IIngestRequestRepository;
import fr.cnes.regards.modules.ingest.dao.ILastAIPRepository;
import fr.cnes.regards.modules.ingest.dao.ILastSIPRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.sip.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.dto.sip.flow.IngestRequestFlowItem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

@Component
public class IngestServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestServiceTest.class);
    public static final String NEW_SIP_S_IN_DATABASE_TEMPLATE = "{} new SIP(s) {} in database";
    public static final String ALL_STATUS = "ALL_STATUS";

    @Autowired
    protected IIngestRequestRepository ingestRequestRepository;

    @Autowired
    protected ISIPRepository sipRepository;

    @Autowired
    protected ILastSIPRepository lastSipRepository;

    @Autowired
    protected IAIPRepository aipRepository;

    @Autowired
    protected ILastAIPRepository lastAipRepository;

    @Autowired
    protected IAbstractRequestRepository requestRepository;

    @Autowired
    private IJobInfoRepository jobInfoRepository;

    @Autowired
    private ISnapshotProcessRepository snapshotProcessRepository;

    @Autowired
    private ISessionStepRepository sessionStepRepository;

    @Autowired
    private IStepPropertyUpdateRequestRepository stepPropertyUpdateRequestRepository;

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

    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Clean everything a test can use, to prepare the empty environment for the next test
     */
    public void init() {
        boolean done = false;
        int loop = 0;
        do {
            try {
                ingestProcessingChainRepository.deleteAllInBatch();
                ingestRequestRepository.deleteAllInBatch();
                requestRepository.deleteAllInBatch();
                lastAipRepository.deleteAllInBatch();
                aipRepository.deleteAllInBatch();
                lastSipRepository.deleteAllInBatch();
                sipRepository.deleteAllInBatch();
                jobInfoRepository.deleteAll();
                pluginConfRepo.deleteAllInBatch();
                snapshotProcessRepository.deleteAllInBatch();
                stepPropertyUpdateRequestRepository.deleteAllInBatch();
                sessionStepRepository.deleteAllInBatch();
                done = waitAllRequestsFinished();
            } catch (DataAccessException e) {
                LOGGER.error(e.getMessage(), e);
            }
            loop++;
        } while (done && (loop < 5));
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
     */
    public void waitForIngestion(long expectedSips, long timeout, SIPState sipState) {
        long end = System.currentTimeMillis() + timeout;
        // Wait
        long sipCount = 0;
        do {
            long newCount = 0;
            long totalCount = sipRepository.count();
            if (sipState != null) {
                newCount = sipRepository.countByState(sipState);
            } else {
                newCount = totalCount;
            }
            if (newCount != sipCount) {
                LOGGER.info(NEW_SIP_S_IN_DATABASE_TEMPLATE, newCount - sipCount,
                            sipState != null ? sipState.toString() : ALL_STATUS);
            }
            sipCount = newCount;
            if (timerStop(expectedSips, end, sipCount, String
                    .format("Timeout after waiting %s ms for %s SIPs in %s. Actual=%s. Total count %s (no specific status)",
                            timeout, expectedSips, sipState != null ? sipState.toString() : ALL_STATUS, sipCount,
                            totalCount))) {
                break;
            }
        } while (true);
    }

    /**
     * Helper method to wait for AIP ingestion
     */
    public void waitForAIP(long expectedSips, long timeout, AIPState aipState) {
        long end = System.currentTimeMillis() + timeout;
        // Wait
        long aipCount = 0;
        do {
            long newCount = 0;
            long totalCount = aipRepository.count();
            if (aipState != null) {
                newCount = aipRepository.countByState(aipState);
            } else {
                newCount = totalCount;
            }
            if (newCount != aipCount) {
                LOGGER.info(NEW_SIP_S_IN_DATABASE_TEMPLATE, newCount - aipCount,
                            aipState != null ? aipState.toString() : ALL_STATUS);
            }
            aipCount = newCount;
            if (timerStop(expectedSips, end, aipCount, String
                    .format("Timeout after waiting for %s SIPs in %s. Acutal=%s. Total count %s (no specific status)",
                            expectedSips, aipState != null ? aipState.toString() : ALL_STATUS, aipCount,
                            totalCount))) {
                break;
            }
        } while (true);
    }

    /**
     * Helper method to wait for ingest request state
     */
    public void waitForIngestRequest(long expectedSips, long timeout, InternalRequestState requestState) {
        long end = System.currentTimeMillis() + timeout;
        // Wait
        long requestCount = 0;
        do {
            long newCount = 0;
            long totalCount = ingestRequestRepository.count();
            if (requestState != null) {
                newCount = ingestRequestRepository.countByState(requestState);
            } else {
                newCount = totalCount;
            }
            if (newCount != requestCount) {
                LOGGER.info(NEW_SIP_S_IN_DATABASE_TEMPLATE, newCount - requestCount,
                            requestState != null ? requestState.toString() : ALL_STATUS);
            }
            requestCount = newCount;
            if (timerStop(expectedSips, end, requestCount, String
                    .format("Timeout after waiting for %s SIPs in %s. Acutal=%s. Total count %s (no specific status)",
                            expectedSips, requestState != null ? requestState.toString() : ALL_STATUS, requestCount,
                            totalCount))) {
                break;
            }
        } while (true);
    }

    private boolean timerStop(long expectedSips, long end, long sipCount, String errorMessage) {
        if (sipCount >= expectedSips) {
            return true;
        }
        long now = System.currentTimeMillis();
        if (end > now) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Assert.fail("Thread interrupted");
            }
        } else {
            Assert.fail(errorMessage);
        }
        return false;
    }

    /**
     * Helper method to wait for DB ingestion
     * @param expectedTasks expected count of task in db
     * @param timeout in ms
     */
    public void waitForRequestReach(long expectedTasks, long timeout) {
        long end = System.currentTimeMillis() + timeout;
        // Wait
        long taskCount;
        do {
            taskCount = abstractRequestRepository.count();
            LOGGER.debug("{} UpdateRequest(s) created in database", taskCount);
            if (timerStop(expectedTasks, end, taskCount, String
                    .format("Timeout after waiting for %s request tasks ends. Actual=%s", expectedTasks, taskCount))) {
                break;
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
            long count = abstractRequestRepository
                    .countByStateIn(Sets.newHashSet(InternalRequestState.BLOCKED, InternalRequestState.CREATED,
                                                    InternalRequestState.RUNNING, InternalRequestState.TO_SCHEDULE));
            LOGGER.info("{} Current request running", count);
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
                Assert.fail("Timeout waiting for all requests finished. Remaining " + count);
            }
        } while (true);
    }

    public boolean waitAllRequestsFinished() {
        try {
            Thread.sleep(1000);
            // Wait
            long count = abstractRequestRepository
                    .countByStateIn(Sets.newHashSet(InternalRequestState.BLOCKED, InternalRequestState.CREATED,
                                                    InternalRequestState.RUNNING, InternalRequestState.TO_SCHEDULE));
            LOGGER.info("{} Current request running", count);
            return count == 0;
        } catch (InterruptedException e) {
            return false;
        }
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
        sendIngestRequestEvent(Sets.newHashSet(sip), mtd);
    }

    public void sendIngestRequestEvent(Collection<SIP> sips, IngestMetadataDto mtd) {
        List<IngestRequestFlowItem> toSend = new ArrayList<>(sips.size());
        for (SIP sip : sips) {
            toSend.add(IngestRequestFlowItem.build(mtd, sip));
        }
        publisher.publish(toSend);
    }

    public void waitJobDone(JobInfo jobInfo, JobStatus status, long timeout) {
        boolean done = false;
        long start = System.currentTimeMillis();
        JobInfo ji = null;
        do {
            ji = jobInfoRepository.findCompleteById(jobInfo.getId());
            if (ji.getStatus().getStatus() == status) {
                done = true;
            }
        }while (!done && (System.currentTimeMillis() < (start + timeout)));

        if (!done) {
            Assert.assertEquals("Job info is not in expected status", ji.getStatus().getStatus(), jobInfo.getStatus().getStatus());
        }
    }

}
