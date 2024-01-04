/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.dto.sip.SIPDto;
import fr.cnes.regards.modules.ingest.dao.*;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.dto.StorageDto;
import fr.cnes.regards.modules.ingest.dto.VersioningMode;
import fr.cnes.regards.modules.ingest.dto.sip.flow.IngestRequestFlowItem;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@ActiveProfiles("test")
public class IngestServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestServiceIT.class);

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

    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Clean everything a test can use, to prepare the empty environment for the next test
     */
    public void init(String tenant) {
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
                done = checkAllRequestsFinished(tenant);
                if (!done) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        LOGGER.error(e.getMessage(), e);
                        done = true;
                    }
                }
            } catch (DataAccessException e) {
                LOGGER.error(e.getMessage(), e);
            }
            loop++;
        } while (!done && (loop < 5));
    }

    public void waitForIngestion(long expectedSips, String tenant) {
        waitForIngestion(expectedSips, expectedSips * 1000, tenant);
    }

    public void waitForIngestion(long expectedSips, long timeout, String tenant) {
        waitForIngestion(expectedSips, timeout, null, tenant);
    }

    /**
     * Helper method to wait for SIP ingestion
     *
     * @param expectedSips expected count of sips in database
     * @param timeout      in ms
     */
    public void waitForIngestion(long expectedSips, long timeout, SIPState sipState, String tenant) {
        long end = System.currentTimeMillis() + timeout;
        try {
            Awaitility.await().atMost(timeout, TimeUnit.MILLISECONDS).until(() -> {
                runtimeTenantResolver.forceTenant(tenant);
                if (sipState != null) {
                    return sipRepository.countByState(sipState) == expectedSips;
                }
                return sipRepository.count() == expectedSips;
            });
        } catch (Exception e) {
            long count = 0;
            if (sipState != null) {
                count = sipRepository.countByState(sipState);
            } else {
                count = sipRepository.count();
            }
            Assert.fail(String.format("Error waiting for %s ingestion done in %s ms. Number of ingestion done = %s",
                                      expectedSips,
                                      timeout,
                                      count));
        }
    }

    /**
     * Helper method to wait for AIP ingestion
     */
    public void waitForAIP(long expectedSips, long timeout, AIPState aipState, String tenant) {
        Awaitility.await().atMost(timeout, TimeUnit.MILLISECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(tenant);
            if (aipState != null) {
                return aipRepository.countByState(aipState) == expectedSips;
            }
            return aipRepository.count() == expectedSips;
        });
    }

    /**
     * Helper method to wait for ingest request state
     */
    public void waitForIngestRequest(long expectedSips,
                                     long timeout,
                                     InternalRequestState requestState,
                                     String tenant) {
        Awaitility.await().atMost(timeout, TimeUnit.MILLISECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(tenant);
            if (requestState != null) {
                return ingestRequestRepository.countByState(requestState) == expectedSips;
            }
            return ingestRequestRepository.count() == expectedSips;
        });
    }

    /**
     * Helper method that waits all requests have been processed
     */
    public void waitAllRequestsFinished(long timeout, String tenant) {
        Awaitility.await().atMost(timeout, TimeUnit.MILLISECONDS).until(() -> checkAllRequestsFinished(tenant));
    }

    public boolean checkAllRequestsFinished(String tenant) {
        runtimeTenantResolver.forceTenant(tenant);
        long count = abstractRequestRepository.countByStateIn(Sets.newHashSet(InternalRequestState.BLOCKED,
                                                                              InternalRequestState.CREATED,
                                                                              InternalRequestState.RUNNING,
                                                                              InternalRequestState.TO_SCHEDULE));
        LOGGER.info("{} Current request running", count);
        return count == 0;
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
     */
    public void sendIngestRequestEvent(SIPDto sip, IngestMetadataDto mtd) {
        sendIngestRequestEvent(Sets.newHashSet(sip), mtd);
    }

    public void sendIngestRequestEvent(Collection<SIPDto> sips, IngestMetadataDto mtd) {
        List<IngestRequestFlowItem> toSend = new ArrayList<>(sips.size());
        for (SIPDto sip : sips) {
            toSend.add(IngestRequestFlowItem.build(mtd, sip));
        }
        publisher.publish(toSend);
    }

    public IngestRequestFlowItem createSipEvent(SIPDto sip,
                                                String storage,
                                                String session,
                                                String sessionOwner,
                                                List<String> categories,
                                                Optional<String> chainLabel,
                                                VersioningMode versioningMode) {
        // Create event
        List<StorageDto> storagesMeta = List.of(new StorageDto(storage));

        IngestMetadataDto mtd = new IngestMetadataDto(sessionOwner,
                                                      session,
                                                      null,
                                                      IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL,
                                                      Sets.newHashSet(categories),
                                                      null,
                                                      null,
                                                      storagesMeta);

        return IngestRequestFlowItem.build(mtd, sip);
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
        } while (!done && (System.currentTimeMillis() < (start + timeout)));

        if (!done) {
            Assert.assertEquals("Job info is not in expected status",
                                ji.getStatus().getStatus(),
                                jobInfo.getStatus().getStatus());
        }
    }

}
