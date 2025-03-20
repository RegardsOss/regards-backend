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
package fr.cnes.regards.modules.crawler.service.handler;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEventType;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.modules.crawler.service.job.UpdateEntityIntoEsJob;
import fr.cnes.regards.modules.crawler.service.service.EntityIndexerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JobEventHandler for dam jobs. Handle failure and abort for {@link UpdateEntityIntoEsJob}
 *
 * @author Thibaud Michaudel
 **/
@Component
public class JobEventHandler implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<JobEvent> {

    private final ISubscriber subscriber;

    private final JobInfoService jobInfoService;

    private final EntityIndexerService entityIndexerService;

    @Value("${regards.dam.job.updateDataset.maxRetry:5}")
    private int nbMaxRetry;

    private Cache<String, AtomicInteger> cacheRetryNumberByUrn = Caffeine.newBuilder().build();

    public JobEventHandler(ISubscriber subscriber,
                           JobInfoService jobInfoService,
                           EntityIndexerService entityIndexerService) {
        this.subscriber = subscriber;
        this.jobInfoService = jobInfoService;
        this.entityIndexerService = entityIndexerService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(JobEvent.class, this);
    }

    @Override
    public void handleBatch(List<JobEvent> messages) {
        long start = System.currentTimeMillis();
        LOGGER.debug("[DAM JOB EVENT HANDLER] Handling {} JobEvents...", messages.size());
        long nbJobError = 0;
        for (JobEvent jobEvent : messages) {
            if (jobEvent.getJobEventType() == JobEventType.FAILED
                || jobEvent.getJobEventType() == JobEventType.ABORTED) {
                JobInfo jobInfo = jobInfoService.retrieveJob(jobEvent.getJobId());
                if (jobInfo.getClassName().equals(UpdateEntityIntoEsJob.class.getName())) {
                    try {
                        nbJobError++;
                        String urn = IJob.getValue(jobInfo.getParametersAsMap(), UpdateEntityIntoEsJob.URN_PARAMETER);
                        Long requestId = IJob.getValue(jobInfo.getParametersAsMap(),
                                                       UpdateEntityIntoEsJob.REQUEST_ID_PARAMETER);
                        // Get retry number for this urn from cache
                        AtomicInteger nbRetry = cacheRetryNumberByUrn.get(urn, key -> new AtomicInteger(0));
                        // Retry if max number of retry is not reached
                        if (nbRetry.incrementAndGet() <= nbMaxRetry) {
                            entityIndexerService.retryEntityUpdateRequests(requestId);
                        } else {
                            entityIndexerService.failedEntityUpdateRequests(requestId);
                        }
                    } catch (JobParameterMissingException | JobParameterInvalidException e) {
                        LOGGER.error("Unable to retrieve urn to retry the job {}", jobInfo.getId(), e);
                    }
                }
            }
        }
        LOGGER.debug("[DAM JOB EVENT HANDLER] {} JobEvents in error handled in {} ms",
                     nbJobError,
                     System.currentTimeMillis() - start);
    }

    @Override
    public Errors validate(JobEvent message) {
        return null;
    }

    @Override
    public boolean isDedicatedDLQEnabled() {
        return false;
    }
}
