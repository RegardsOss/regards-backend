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

import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.modules.ingest.dao.IAIPDumpMetadataRepositoryRefactor;
import fr.cnes.regards.modules.ingest.dao.IAIPSaveMetadataRepositoryRefactor;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPSaveMetadataRequestRefactor;
import fr.cnes.regards.modules.ingest.service.job.AIPSaveMetaDataJob;
import fr.cnes.regards.modules.ingest.service.job.AIPSaveMetadataJobRefactor;
import fr.cnes.regards.modules.ingest.service.job.IngestJobPriority;

/**
 * Service to handle {@link AIPSaveMetaDataJob}s
 * @author Iliana Ghazali
 *
 */
@Service
@MultitenantTransactional
public class AIPSaveMetadataServiceRefactor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPSaveMetadataServiceRefactor.class);

    @Autowired
    private IAIPSaveMetadataRepositoryRefactor aipSaveMetadataRepositoryRefactor;

    @Autowired
    private IAIPDumpMetadataRepositoryRefactor aipDumpMetadataRepositoryRefactor;

    @Autowired
    private JobInfoService jobInfoService;

    /**
     * Schedule Jobs
     */
    public void scheduleJobs() {
        LOGGER.trace("[SAVE METADATA SCHEDULER] Scheduling job ...");
        long start = System.currentTimeMillis();

        // Create request
        // find last dump date and create request
        OffsetDateTime lastDumpDate = aipDumpMetadataRepositoryRefactor.findLastDumpDate();
        AIPSaveMetadataRequestRefactor aipSaveMetadataRequest = new AIPSaveMetadataRequestRefactor(lastDumpDate);
        aipSaveMetadataRequest.setState(InternalRequestState.RUNNING);
        aipSaveMetadataRepositoryRefactor.save(aipSaveMetadataRequest);

        // Schedule save metadata job
        JobInfo jobInfo = new JobInfo(false, IngestJobPriority.AIP_SAVE_METADATA_RUNNER_PRIORITY.getPriority(),
                                      Sets.newHashSet(new JobParameter(AIPSaveMetadataJobRefactor.SAVE_METADATA_REQUEST,
                                                                       aipSaveMetadataRequest)), null,
                                      AIPSaveMetadataJobRefactor.class.getName());
        jobInfoService.createAsQueued(jobInfo);
        LOGGER.debug("[SAVE METADATA SCHEDULER] 1 Job scheduled for 1 AIPSaveMetaDataRequest(s) in {} ms",
                     System.currentTimeMillis() - start);

    }

}
