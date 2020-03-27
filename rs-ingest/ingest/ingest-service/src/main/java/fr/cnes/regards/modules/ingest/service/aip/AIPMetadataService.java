/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.aip;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.modules.ingest.dao.IAIPStoreMetaDataRepository;
import fr.cnes.regards.modules.ingest.dao.IAbstractRequestRepository;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPStoreMetaDataRequest;
import fr.cnes.regards.modules.ingest.service.job.AIPSaveMetaDataJob;
import fr.cnes.regards.modules.ingest.service.job.IngestJobPriority;

/**
 * Service to handle {@link AIPSaveMetaDataJob}s
 *
 * @author Léo Mieulet
 * @author Sébastien Binda
 *
 */
@Service
@MultitenantTransactional
public class AIPMetadataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPMetadataService.class);

    @Autowired
    private IAIPStoreMetaDataRepository aipStoreMetaDataRepository;

    @Autowired
    private IAbstractRequestRepository abstractRequestRepository;

    @Autowired
    private JobInfoService jobInfoService;

    /**
     * Limit number of AIPs to retrieve in one page.
     */
    @Value("${regards.aips.save-metadata.scan.iteration-limit:100}")
    private Integer updateRequestIterationLimit;

    /**
     * Schedule Jobs
     */
    public boolean scheduleJobs() {
        JobInfo jobInfo = null;
        LOGGER.trace("[OAIS SAVE METADATA SCHEDULER] Scheduling job ...");
        long start = System.currentTimeMillis();
        Pageable pageRequest = PageRequest.of(0, updateRequestIterationLimit, Sort.Direction.ASC, "id");
        // Fetch the first list of update request to handle
        Page<AIPStoreMetaDataRequest> waitingRequests = aipStoreMetaDataRepository.findWaitingRequest(pageRequest);
        if (!waitingRequests.isEmpty()) {
            List<AIPStoreMetaDataRequest> content = waitingRequests.getContent();

            // Make a list of request ids
            List<Long> requestIds = content.stream().map(AIPStoreMetaDataRequest::getId).collect(Collectors.toList());

            // Schedule deletion job
            Set<JobParameter> jobParameters = Sets.newHashSet();
            jobParameters.add(new JobParameter(AIPSaveMetaDataJob.UPDATE_METADATA_REQUEST_IDS, requestIds));
            jobInfo = new JobInfo(false, IngestJobPriority.AIP_SAVE_METADATA_RUNNER_PRIORITY.getPriority(),
                    jobParameters, null, AIPSaveMetaDataJob.class.getName());
            jobInfoService.createAsQueued(jobInfo);

            // Change request state
            abstractRequestRepository.updateStates(requestIds, InternalRequestState.RUNNING);

            LOGGER.info("[OAIS SAVE METADATA SCHEDULER] 1 Job scheduled for {} AIPStoreMetaDataRequest(s) in {} ms",
                        waitingRequests.getNumberOfElements(), System.currentTimeMillis() - start);
        }
        return waitingRequests.hasNext();
    }

}
