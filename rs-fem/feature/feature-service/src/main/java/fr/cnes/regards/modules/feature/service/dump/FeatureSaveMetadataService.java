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


package fr.cnes.regards.modules.feature.service.dump;

import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.event.AbstractRequestEvent;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.dump.domain.DumpSettings;
import fr.cnes.regards.framework.modules.dump.service.settings.IDumpSettingsService;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.modules.feature.dao.IFeatureSaveMetadataRequestRepository;
import fr.cnes.regards.modules.feature.domain.request.FeatureSaveMetadataRequest;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.service.job.FeatureSaveMetadataJob;

/**
 * Service to handle {@link FeatureSaveMetadataJob}
 * @author Iliana Ghazali
 */

@Service
@MultitenantTransactional
public class FeatureSaveMetadataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureSaveMetadataService.class);

    @Autowired
    private IFeatureSaveMetadataRequestRepository metadataRequestRepository;

    @Autowired
    private IDumpSettingsService dumpSettingsService;

    @Autowired
    private JobInfoService jobInfoService;

    /**
     * Schedule Jobs
     */
    public JobInfo scheduleJobs() {
        LOGGER.trace("[DUMP SCHEDULER] Scheduling job ...");
        long start = System.currentTimeMillis();
        JobInfo jobInfo = null;

        // Update lastDumpReqDate
        DumpSettings lastDump = dumpSettingsService.retrieve();
        OffsetDateTime lastDumpDate = lastDump.getLastDumpReqDate();
        lastDump.setLastDumpReqDate(OffsetDateTime.now());

        dumpSettingsService.update(lastDump);

        // Create request
        FeatureSaveMetadataRequest requestToSchedule = FeatureSaveMetadataRequest
                .build(AbstractRequestEvent.generateRequestId(), "NONE", OffsetDateTime.now(), RequestState.GRANTED,
                       null, FeatureRequestStep.LOCAL_SCHEDULED, PriorityLevel.NORMAL, lastDumpDate,
                       lastDump.getDumpLocation());

        metadataRequestRepository.save(requestToSchedule);

        // Schedule save metadata job
        jobInfo = new JobInfo(false, requestToSchedule.getPriority().getPriorityLevel(),
                              Sets.newHashSet(new JobParameter(FeatureSaveMetadataJob.SAVE_METADATA_REQUEST,
                                                               requestToSchedule)), null,
                              FeatureSaveMetadataJob.class.getName());
        jobInfoService.createAsQueued(jobInfo);
        LOGGER.debug("[SAVE METADATA SCHEDULER] 1 Job scheduled for 1 FeatureSaveMetaDataRequest(s) in {} ms",
                     System.currentTimeMillis() - start);
        return jobInfo;
    }

}
