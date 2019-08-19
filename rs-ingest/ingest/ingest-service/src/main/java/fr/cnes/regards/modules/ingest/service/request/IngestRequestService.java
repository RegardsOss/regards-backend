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
package fr.cnes.regards.modules.ingest.service.request;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.locks.service.ILockService;
import fr.cnes.regards.modules.ingest.dao.IIngestProcessingChainRepository;
import fr.cnes.regards.modules.ingest.dao.IIngestRequestRepository;
import fr.cnes.regards.modules.ingest.domain.request.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.sip.IngestProcessingChainView;
import fr.cnes.regards.modules.ingest.dto.request.RequestState;
import fr.cnes.regards.modules.ingest.service.job.IngestJobPriority;
import fr.cnes.regards.modules.ingest.service.job.IngestProcessingJob;

/**
 * Manage ingest requests
 *
 * @author Marc SORDI
 *
 */
@Service
@MultitenantTransactional
public class IngestRequestService implements IIngestRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestRequestService.class);

    private static final String GRANTED_REQUEST_LOCK = "GRANTED_REQUEST_LOCK";

    @Autowired
    private IIngestRequestService self;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private ILockService lockService;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IIngestRequestRepository ingestRequestRepository;

    @Autowired
    private IIngestProcessingChainRepository ingestChainRepository;

    @Value("${regards.ingest.request.job.bulk:100}")
    private Integer bulkRequestLimit;

    @Override
    public void scheduleIngestProcessingJob() {

        // FIXME resolve lock issue! do not work!
        // Prevent concurrent call
        if (lockService.obtainLockOrSkip(GRANTED_REQUEST_LOCK, this, 600)) {
            try {
                ingestChainRepository.findNamesBy()
                        .forEach(chainView -> self.scheduleIngestProcessingJobByChain(chainView));
            } finally {
                lockService.releaseLock(GRANTED_REQUEST_LOCK, this);
            }
        }

    }

    /**
     * Schedule ingest processing jobs for a specified ingestion chain
     * @param chainView chain to consider
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void scheduleIngestProcessingJobByChain(IngestProcessingChainView chainView) {

        // Get granted request(s) per chain and page
        Page<IngestRequest> requests = ingestRequestRepository
                .findPageByMetadataIngestChainAndState(chainView.getName(), RequestState.GRANTED,
                                                       PageRequest.of(0, bulkRequestLimit));

        // Request found
        if (requests.hasContent()) {
            // Schedule jobs
            LOGGER.debug("Scheduling job to handle {} ingest request(s) on chain {}", requests.getNumberOfElements(),
                         chainView.getName());

            Set<Long> ids = requests.get().map(r -> r.getId()).collect(Collectors.toSet());

            Set<JobParameter> jobParameters = Sets.newHashSet();
            jobParameters.add(new JobParameter(IngestProcessingJob.IDS_PARAMETER, ids));
            jobParameters.add(new JobParameter(IngestProcessingJob.CHAIN_NAME_PARAMETER, chainView.getName()));
            JobInfo jobInfo = new JobInfo(false, IngestJobPriority.INGEST_PROCESSING_JOB_PRIORITY.getPriority(),
                    jobParameters, authResolver.getUser(), IngestProcessingJob.class.getName());
            jobInfoService.createAsQueued(jobInfo);

            // Switch request status (same transaction)
            ingestRequestRepository.updateIngestRequestState(RequestState.PENDING, ids);
        }

        // At least one request remains!
        if (requests.hasNext()) {
            // self.scheduleIngestProcessingJobByChain(chainView);
        }
    }

    /* (non-Javadoc)
     * @see fr.cnes.regards.modules.ingest.service.request.IIngestRequestService#getIngestRequests(java.util.Set)
     */
    @Override
    public List<IngestRequest> getIngestRequests(Set<Long> ids) {
        return ingestRequestRepository.findByIdIn(ids);
    }

    /* (non-Javadoc)
     * @see fr.cnes.regards.modules.ingest.service.request.IIngestRequestService#updateIngestRequest(fr.cnes.regards.modules.ingest.domain.request.IngestRequest)
     */
    @Override
    public IngestRequest updateIngestRequest(IngestRequest request) {
        return ingestRequestRepository.save(request);
    }

    /* (non-Javadoc)
     * @see fr.cnes.regards.modules.ingest.service.request.IIngestRequestService#deleteIngestRequest(fr.cnes.regards.modules.ingest.domain.request.IngestRequest)
     */
    @Override
    public void deleteIngestRequest(IngestRequest request) {
        ingestRequestRepository.deleteById(request.getId());
    }
}
