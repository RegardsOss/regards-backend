/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.job;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEventType;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.ingest.service.AipDisseminationService;
import fr.cnes.regards.modules.ingest.service.request.IIngestRequestService;
import fr.cnes.regards.modules.ingest.service.request.IOAISDeletionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.List;

/**
 * Job handler on ingest
 *
 * @author Marc SORDI
 * @author Léo Mieulet
 */
@Component
public class JobEventHandler implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<JobEvent> {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(JobEventHandler.class);

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IIngestRequestService ingestRequestService;

    @Autowired
    private IOAISDeletionService oaisDeletionService;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private AipDisseminationService aipDisseminationService;

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
        LOGGER.debug("[INGEST JOB EVENT HANDLER] Handling {} JobEvents...", jobEvents.size());
        long nbJobError = 0;
        for (JobEvent jobEvent : jobEvents) {
            if (jobEvent.getJobEventType() == JobEventType.FAILED
                || jobEvent.getJobEventType() == JobEventType.ABORTED) {
                JobInfo jobInfo = jobInfoService.retrieveJob(jobEvent.getJobId());
                // Keep in mind a single request that fail does not mean the job will have the FAILED state
                // we receive here events when the job raises an exception on boot / end (issue with params, plugin init ...)
                // so all requests are dead
                boolean isHandled = ingestRequestService.handleJobCrash(jobInfo) || oaisDeletionService.handleJobCrash(
                    jobInfo) || aipDisseminationService.handleJobCrash(jobInfo);
                if (isHandled) {
                    nbJobError++;
                }
            }
        }
        LOGGER.debug("[INGEST JOB EVENT HANDLER] {} JobEvents in error handled in {} ms",
                     nbJobError,
                     System.currentTimeMillis() - start);
    }

    @Override
    public boolean isDedicatedDLQEnabled() {
        return false;
    }
}