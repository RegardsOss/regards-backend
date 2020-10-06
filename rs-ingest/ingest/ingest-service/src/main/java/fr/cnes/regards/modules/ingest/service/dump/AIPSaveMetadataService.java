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
package fr.cnes.regards.modules.ingest.service.dump;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.modules.ingest.dao.IDumpConfigurationRepository;
import fr.cnes.regards.modules.ingest.dao.IAIPSaveMetadataRequestRepository;
import fr.cnes.regards.modules.ingest.domain.dump.DumpConfiguration;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.dump.AIPSaveMetadataRequest;
import fr.cnes.regards.modules.ingest.service.job.AIPSaveMetadataJob;
import fr.cnes.regards.modules.ingest.service.job.IngestJobPriority;

/**
 * Service to handle {@link AIPSaveMetadataJob}s
 * @author Iliana Ghazali
 * @author Sylvain VISSIERE-GUERINET
 */
@Service
@MultitenantTransactional
public class AIPSaveMetadataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPSaveMetadataService.class);

    @Autowired
    private IAIPSaveMetadataRequestRepository metadataRequestRepository;

    @Autowired
    private IDumpConfigurationRepository dumpRepository;

    @Autowired
    private JobInfoService jobInfoService;

    @Value("${regards.dump.aips.cron.expression:0 0 0 ? * 1#1 *}")
    private String cron;

    /**
     * Schedule Jobs
     */
    public JobInfo scheduleJobs() {
        LOGGER.trace("[SAVE METADATA SCHEDULER] Scheduling job ...");
        long start = System.currentTimeMillis();

        // Find dump configuration
        Optional<DumpConfiguration> lastDumpOpt = dumpRepository.findById(DumpConfiguration.DUMP_CONF_ID);
        DumpConfiguration lastDump;
        if (!lastDumpOpt.isPresent()) {
            lastDump = new DumpConfiguration(true, this.cron, null, null);
            dumpRepository.save(lastDump);
        } else{
            lastDump = lastDumpOpt.get();
        }

        // Update lastDumpReqDate
        OffsetDateTime lastDumpDate = lastDump.getLastDumpReqDate();
        lastDump.setLastDumpReqDate(OffsetDateTime.now());
        dumpRepository.save(lastDump);

        // Create request
        AIPSaveMetadataRequest aipSaveMetadataRequest = new AIPSaveMetadataRequest(lastDumpDate,
                                                                                   lastDump.getDumpLocation());
        aipSaveMetadataRequest.setState(InternalRequestState.RUNNING);
        metadataRequestRepository.save(aipSaveMetadataRequest);

        // Schedule save metadata job
        JobInfo jobInfo = new JobInfo(false, IngestJobPriority.AIP_SAVE_METADATA_RUNNER_PRIORITY.getPriority(),
                                      Sets.newHashSet(new JobParameter(AIPSaveMetadataJob.SAVE_METADATA_REQUEST,
                                                                       aipSaveMetadataRequest)), null,
                                      AIPSaveMetadataJob.class.getName());
        jobInfoService.createAsQueued(jobInfo);
        LOGGER.debug("[SAVE METADATA SCHEDULER] 1 Job scheduled for 1 AIPSaveMetaDataRequest(s) in {} ms",
                     System.currentTimeMillis() - start);

        return jobInfo;
    }
}
