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
package fr.cnes.regards.modules.notifier.service.job;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEventType;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.notifier.service.NotificationRegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.List;
import java.util.Set;

/**
 * Job event handlers.
 *
 * @author Sébastien Binda
 */
@Component
public class JobEventHandler implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<JobEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobEventHandler.class);

    private final ISubscriber subscriber;

    private final IJobInfoService jobInfoService;

    private final NotificationRegistrationService notificationRegistrationService;

    public JobEventHandler(ISubscriber subscriber,
                           IJobInfoService jobInfoService,
                           NotificationRegistrationService notificationRegistrationService) {
        this.subscriber = subscriber;
        this.jobInfoService = jobInfoService;
        this.notificationRegistrationService = notificationRegistrationService;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        subscriber.subscribeTo(JobEvent.class, this);
    }

    @Override
    public Errors validate(JobEvent message) {
        return null;
    }

    @Override
    public void handleBatch(List<JobEvent> jobEvents) {
        long start = System.currentTimeMillis();
        LOGGER.debug("[NOTIFIER JOB EVENT HANDLER] Handling {} JobEvents...", jobEvents.size());
        long nbJobError = 0;
        for (JobEvent jobEvent : jobEvents) {
            if (jobEvent.getJobEventType() == JobEventType.FAILED
                || jobEvent.getJobEventType() == JobEventType.ABORTED) {
                JobInfo jobInfo = jobInfoService.retrieveJob(jobEvent.getJobId());
                // If failed job is a NotificationJob so handle crash for associated requests
                if (NotificationJob.class.getName().equals(jobInfo.getClassName())) {
                    Set<Long> requestIds = jobInfo.getParametersAsMap()
                                                  .get(NotificationJob.NOTIFICATION_REQUEST_IDS)
                                                  .getValue();
                    String recipientBusinessId = jobInfo.getParametersAsMap()
                                                .get(NotificationJob.RECIPIENT_BUSINESS_ID)
                                                .getValue();
                    notificationRegistrationService.handleJobCrash(requestIds, recipientBusinessId);
                }
            }
        }
        LOGGER.debug("[NOTIFIER JOB EVENT HANDLER] {} JobEvents in error handled in {} ms",
                     nbJobError,
                     System.currentTimeMillis() - start);
    }
}