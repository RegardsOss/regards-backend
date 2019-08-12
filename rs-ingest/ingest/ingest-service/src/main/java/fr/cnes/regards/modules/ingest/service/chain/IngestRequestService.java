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
package fr.cnes.regards.modules.ingest.service.chain;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import fr.cnes.regards.modules.ingest.dao.IIngestProcessingChainRepository;
import fr.cnes.regards.modules.ingest.dao.IIngestRequestRepository;
import fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChainView;
import fr.cnes.regards.modules.ingest.domain.entity.request.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.entity.request.IngestRequestState;
import fr.cnes.regards.modules.ingest.service.IngestProperties;
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
public class IngestRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestRequestService.class);

    // FIXME is it a proxy to properly handle transaction
    @Autowired
    private IngestRequestService self;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IIngestRequestRepository ingestRequestRepository;

    @Autowired
    private IIngestProcessingChainRepository ingestChainRepository;

    // FIXME manage concurrent access  on ingest request table! lock on schedule!
    /**
     * Schedul ingest processing jobs
     */
    public void scheduleIngestProcessingJob() {
        ingestChainRepository.findAllNames().forEach(chainView -> scheduleIngestProcessingJobByChain(chainView));
    }

    /**
     * Schedul ingest processing jobs for a specified ingestion chain
     * @param chainView chain to consider
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void scheduleIngestProcessingJobByChain(IngestProcessingChainView chainView) {

        // Get granted request(s) per chain and page
        Page<IngestRequest> requests = ingestRequestRepository
                .findPageByMetadataIngestChainAndState(chainView.getName(), IngestRequestState.GRANTED,
                                                       PageRequest.of(0, IngestProperties.WORKING_UNIT));

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
            ingestRequestRepository.updateIngestRequestState(IngestRequestState.PENDING, ids);
        }

        // At least one request remains!
        if (requests.hasNext()) {
            self.scheduleIngestProcessingJobByChain(chainView);
        }
    }

    /**
     * Load a collection of requests
     */
    public List<IngestRequest> getIngestRequests(Set<Long> ids) {
        return ingestRequestRepository.findByIdIn(ids);
    }

    /**
     * Update a request
     */
    public IngestRequest updateIngestRequest(IngestRequest request) {
        return ingestRequestRepository.save(request);
    }

    /**
     * Delete successful request
     * @param request
     */
    public void deleteIngestRequest(IngestRequest request) {
        ingestRequestRepository.deleteById(request.getId());
    }
}
