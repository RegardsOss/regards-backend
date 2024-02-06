/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.service.file.job;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEventType;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.storage.service.file.request.FileCacheRequestService;
import fr.cnes.regards.modules.storage.service.file.request.FileDeletionRequestService;
import fr.cnes.regards.modules.storage.service.file.request.FileStorageRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.List;

/**
 * Job handler on storage
 *
 * @author Léo Mieulet
 */
@Component
public class JobEventHandler implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<JobEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobEventHandler.class);

    private ISubscriber subscriber;

    private FileStorageRequestService fileStorageRequestService;

    private FileDeletionRequestService fileDeletionRequestService;

    private FileCacheRequestService fileCacheRequestService;

    private IJobInfoService jobInfoService;

    public JobEventHandler(ISubscriber subscriber,
                           IJobInfoService jobInfoService,
                           FileStorageRequestService fileStorageRequestService,
                           FileDeletionRequestService fileDeletionRequestService,
                           FileCacheRequestService fileCacheRequestService) {
        this.subscriber = subscriber;
        this.jobInfoService = jobInfoService;
        this.fileStorageRequestService = fileStorageRequestService;
        this.fileDeletionRequestService = fileDeletionRequestService;
        this.fileCacheRequestService = fileCacheRequestService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(JobEvent.class, this);
    }

    @Override
    public Errors validate(JobEvent message) {
        return null;
    }

    @Override
    public void handleBatch(List<JobEvent> jobEvents) {
        long start = System.currentTimeMillis();
        LOGGER.debug("[STORAGE JOB EVENT HANDLER] Handling {} JobEvents...", jobEvents.size());
        long nbJobError = 0;
        for (JobEvent jobEvent : jobEvents) {
            if (jobEvent.getJobEventType() == JobEventType.FAILED
                || jobEvent.getJobEventType() == JobEventType.ABORTED) {
                JobInfo jobInfo = jobInfoService.retrieveJob(jobEvent.getJobId());
                // Keep in mind a single request that fail does not mean the job will have the FAILED state
                // we receive here events when the job raises an exception on boot / end (issue with params, plugin init ...)
                // so all requests are dead
                boolean isHandled = fileStorageRequestService.handleJobCrash(jobInfo)
                                    || fileDeletionRequestService.handleJobCrash(jobInfo)
                                    || fileCacheRequestService.handleJobCrash(jobInfo);
                if (isHandled) {
                    nbJobError++;
                }
            }
        }
        LOGGER.debug("[STORAGE JOB EVENT HANDLER] {} JobEvents in error handled in {} ms",
                     nbJobError,
                     System.currentTimeMillis() - start);
    }
}