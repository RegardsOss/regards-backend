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
package fr.cnes.regards.modules.ingest.service.store;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEventType;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.domain.entity.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.domain.entity.SipAIPState;
import fr.cnes.regards.modules.ingest.service.ISIPService;
import fr.cnes.regards.modules.ingest.service.chain.IIngestProcessingService;
import fr.cnes.regards.modules.ingest.service.job.AIPSubmissionJob;
import fr.cnes.regards.modules.storage.client.IAipEntityClient;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.IAipState;
import fr.cnes.regards.modules.storage.domain.event.AIPEvent;

/**
 * Service to handle aip related issues in ingest, including sending bulk request of AIP to store to archival storage
 * microservice.
 * @author Sébastien Binda
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 */
@Service
@MultitenantTransactional
public class AIPService implements IAIPService {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(AIPService.class);

    @Autowired
    private ISIPService sipService;

    @Autowired
    private ISIPRepository sipRepository;

    @Autowired
    private IAipEntityClient aipEntityClient;

    @Autowired
    private IAIPRepository aipRepository;

    @Autowired
    private IIngestProcessingService processingService;

    @Autowired
    private IJobInfoService jobInfoService;

    @Value("${regards.ingest.aips.bulk.request.limit:1000}")
    private Integer bulkRequestLimit;

    @Override
    public void setAipInError(String ipId, IAipState state, String errorMessage) {
        Optional<AIPEntity> oAip = aipRepository.findByIpId(ipId);
        if (oAip.isPresent()) {
            // Update AIP State
            AIPEntity aip = oAip.get();
            aipRepository.updateAIPEntityStateAndErrorMessage(state, ipId, errorMessage);
            // Update SIP associated State
            SIPEntity sip = aip.getSip();
            sip.setState(SIPState.STORE_ERROR);
            sipService.saveSIPEntity(sip);
        }
    }

    @Override
    public void setAipToStored(String ipId, IAipState state) {
        // Retrieve aip and set the new status to stored
        Optional<AIPEntity> oAip = aipRepository.findByIpId(ipId);
        if (oAip.isPresent()) {
            AIPEntity aip = oAip.get();
            aip.setState(state);
            aip.setErrorMessage(null);
            aipRepository.save(aip);
            // If all AIP are stored update SIP state to STORED
            Set<AIPEntity> sipAips = aipRepository.findBySip(aip.getSip());
            if (sipAips.stream()
                    .allMatch(a -> AIPState.STORED.equals(a.getState()) || SipAIPState.INDEXED.equals(a.getState()))) {
                SIPEntity sip = aip.getSip();
                sip.setState(SIPState.STORED);
                sipService.saveSIPEntity(sip);
            }
        }
    }

    @Override
    public void deleteAip(String ipId, String sipIpId, IAipState state) {
        // Check if deleted AIP exists in internal database
        Optional<AIPEntity> oAip = aipRepository.findByIpId(ipId);
        if (oAip.isPresent()) {
            // Delete aip
            aipRepository.delete(oAip.get());
        }
        // Retrieve all AIP associated to the SIP.
        FeignSecurityManager.asSystem();
        ResponseEntity<PagedResources<Resource<fr.cnes.regards.modules.storage.domain.database.AIPEntity>>> result = aipEntityClient
                .retrieveAIPEntities(sipIpId, 0, 100);
        FeignSecurityManager.reset();
        if (result.getStatusCode().equals(HttpStatus.OK) && (result.getBody() != null)) {
            Optional<SIPEntity> oSip = sipRepository.findOneByIpId(sipIpId);
            if (oSip.isPresent()) {
                SIPEntity sip = oSip.get();
                // If all AIPs are deleted, update sip to DELETED state
                if (result.getBody().getContent().stream()
                        .allMatch(resource -> fr.cnes.regards.modules.storage.domain.AIPState.DELETED
                                .equals(resource.getContent().getState()))) {
                    sip.setState(SIPState.DELETED);
                } else {
                    // Else update sip to incomplete
                    sip.setState(SIPState.INCOMPLETE);
                }
                sip.setLastUpdateDate(OffsetDateTime.now());
                sipService.saveSIPEntity(sip);
            }
        }
    }

    @Override
    public Optional<AIPEntity> searchAip(UniformResourceName ipId) {
        return aipRepository.findByIpId(ipId.toString());
    }

    @Override
    public AIPEntity setAipToIndexed(AIPEntity aip) {
        aip.setState(SipAIPState.INDEXED);
        aip.setErrorMessage(null);
        aipRepository.save(aip);
        // If all AIP are stored update SIP state to STORED
        Set<AIPEntity> sipAips = aipRepository.findBySip(aip.getSip());
        if (sipAips.stream().allMatch(a -> SipAIPState.INDEXED.equals(a.getState()))) {
            SIPEntity sip = aip.getSip();
            sip.setState(SIPState.INDEXED);
            sipService.saveSIPEntity(sip);
            // AIPs are no longer usefull here we can delete them
            aipRepository.delete(sipAips);
        }
        return aip;
    }

    /**
     * This method is called by a time scheduler. We only schedule on job per ingest chain if and
     * only if an existing job not already exists. To detect that a job is already scheduled, we check the AIP state of
     * the chain. AIPs not already scheduled will be scheduled on next scheduler call.
     */
    @Override
    public void scheduleAIPStorageBulkRequest() {
        // Find all processing chains
        List<IngestProcessingChain> ipcs = processingService.findAll();
        // For each processing chain
        for (IngestProcessingChain ipc : ipcs) {
            // Check if submission not already scheduled
            if (!aipRepository.isAlreadyWorking(ipc.getName())) {
                Page<AIPEntity> page = aipRepository
                        .findWithLockBySipProcessingAndState(ipc.getName(), SipAIPState.CREATED,
                                                             new PageRequest(0, bulkRequestLimit));
                if (page.hasContent()) {
                    // Schedule AIP page submission
                    for (AIPEntity aip : page.getContent()) {
                        aip.setState(SipAIPState.SUBMISSION_SCHEDULED);
                        aipRepository.save(aip);
                    }
                    // Schedule job
                    Set<JobParameter> jobParameters = Sets.newHashSet();
                    jobParameters.add(new JobParameter(AIPSubmissionJob.INGEST_CHAIN_PARAMETER, ipc.getName()));

                    JobInfo jobInfo = new JobInfo(false);
                    jobInfo.setParameters(jobParameters);
                    jobInfo.setClassName(AIPSubmissionJob.class.getName());
                    jobInfoService.createAsQueued(jobInfo);
                }
            }
        }
    }

    @Override
    public void handleJobEvent(JobEvent jobEvent) {
        if (JobEventType.FAILED.equals(jobEvent.getJobEventType())) {
            // Load job info
            JobInfo jobInfo = jobInfoService.retrieveJob(jobEvent.getJobId());
            handleAIPSubmissiontError(jobInfo);
        }
    }

    private void handleAIPSubmissiontError(JobInfo jobInfo) {
        if (AIPSubmissionJob.class.getName().equals(jobInfo.getClassName())) {
            Map<String, JobParameter> params = jobInfo.getParametersAsMap();
            String ingestChain = params.get(AIPSubmissionJob.INGEST_CHAIN_PARAMETER).getValue();
            // Set to submission error
            Set<AIPEntity> aips = aipRepository.findBySipProcessingAndState(ingestChain,
                                                                            SipAIPState.SUBMISSION_SCHEDULED);
            for (AIPEntity aip : aips) {
                setAipInError(aip.getIpId(), SipAIPState.SUBMISSION_ERROR, "Submission job error");
            }
        }
    }

    @Override
    public void handleAipEvent(AIPEvent aipEvent) {
        switch (aipEvent.getAipState()) {
            case STORAGE_ERROR:
                setAipInError(aipEvent.getIpId(), aipEvent.getAipState(), aipEvent.getFailureCause());
                break;
            case STORED:
                setAipToStored(aipEvent.getIpId(), aipEvent.getAipState());
                break;
            case DELETED:
                deleteAip(aipEvent.getIpId(), aipEvent.getSipId(), aipEvent.getAipState());
                break;
            case PENDING:
            case STORING_METADATA:
            case UPDATED:
            case VALID:
            default:
                break;
        }
    }

    @Override
    public AIPEntity save(AIPEntity entity) {
        return aipRepository.save(entity);
    }

    @Override
    public Set<AIPEntity> findAIPToSubmit(String ingestProcessingChain) {
        return aipRepository.findBySipProcessingAndState(ingestProcessingChain, SipAIPState.SUBMISSION_SCHEDULED);
    }
}
