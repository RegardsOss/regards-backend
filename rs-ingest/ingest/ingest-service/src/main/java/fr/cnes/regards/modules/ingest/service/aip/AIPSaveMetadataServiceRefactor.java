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
import java.util.Optional;

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
import fr.cnes.regards.modules.ingest.dao.IAIPSaveMetadataRequestRepositoryRefactor;
import fr.cnes.regards.modules.ingest.domain.dump.LastDump;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.dump.AIPSaveMetadataRequestRefactor;
import fr.cnes.regards.modules.ingest.service.job.AIPSaveMetadataJobRefactor;
import fr.cnes.regards.modules.ingest.service.job.IngestJobPriority;

/**
 * Service to handle {@link AIPSaveMetadataJobRefactor}s
 * @author Iliana Ghazali
 * @author Sylvain VISSIERE-GUERINET
 */
@Service
@MultitenantTransactional
public class AIPSaveMetadataServiceRefactor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPSaveMetadataServiceRefactor.class);

    @Autowired
    private IAIPSaveMetadataRequestRepositoryRefactor requestMetadataRepository;

    @Autowired
    private IAIPDumpMetadataRepositoryRefactor dumpRepository;

    @Autowired
    private JobInfoService jobInfoService;

    /**
     * Schedule Jobs
     */
    public JobInfo scheduleJobs() {
        LOGGER.trace("[SAVE METADATA SCHEDULER] Scheduling job ...");
        long start = System.currentTimeMillis();

        // Find last dump date
        Optional<LastDump> lastDumpOpt = dumpRepository.findById(LastDump.LAST_DUMP_DATE_ID);
        OffsetDateTime lastDumpReqDate = null;
        LastDump lastDump = new LastDump();
        if (lastDumpOpt.isPresent()) {
            lastDump = lastDumpOpt.get();
            lastDumpReqDate = lastDump.getLastDumpReqDate();
        }
        // update lastDumpReqDate
        lastDump.setLastDumpReqDate(OffsetDateTime.now());
        dumpRepository.save(lastDump);

        // Create request
        AIPSaveMetadataRequestRefactor aipSaveMetadataRequest = new AIPSaveMetadataRequestRefactor(lastDumpReqDate);
        aipSaveMetadataRequest.setState(InternalRequestState.RUNNING);
        requestMetadataRepository.save(aipSaveMetadataRequest);

        // Schedule save metadata job
        JobInfo jobInfo = new JobInfo(false, IngestJobPriority.AIP_SAVE_METADATA_RUNNER_PRIORITY.getPriority(),
                                      Sets.newHashSet(new JobParameter(AIPSaveMetadataJobRefactor.SAVE_METADATA_REQUEST,
                                                                       aipSaveMetadataRequest)), null,
                                      AIPSaveMetadataJobRefactor.class.getName());
        jobInfoService.createAsQueued(jobInfo);
        LOGGER.debug("[SAVE METADATA SCHEDULER] 1 Job scheduled for 1 AIPSaveMetaDataRequest(s) in {} ms",
                     System.currentTimeMillis() - start);
        return jobInfo;
    }
}
