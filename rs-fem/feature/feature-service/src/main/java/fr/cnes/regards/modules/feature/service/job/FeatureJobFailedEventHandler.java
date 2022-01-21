/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.feature.service.job;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEventType;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.service.request.IFeatureRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Handler to retrieve jobs that failed to update associated requests if any.
 *
 * @author Sébastien Binda
 */
@Component
public class FeatureJobFailedEventHandler implements IHandler<JobEvent>, ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureJobFailedEventHandler.class);

    private static final List<String> JOB_TYPES = Lists.newArrayList(FeatureCopyJob.class.getName(),
                                                                     FeatureUpdateJob.class.getName(),
                                                                     FeatureDeletionJob.class.getName(),
                                                                     FeatureCreationJob.class.getName());

    private final ISubscriber subscriber;

    private final IFeatureRequestService requestService;

    private final IJobInfoService jobService;

    private final IFeatureRequestService featureRequestService;

    public FeatureJobFailedEventHandler(ISubscriber subscriber, IFeatureRequestService requestService, IJobInfoService jobService,
            IFeatureRequestService featureRequestService) {
        this.subscriber = subscriber;
        this.requestService = requestService;
        this.jobService = jobService;
        this.featureRequestService = featureRequestService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        subscriber.subscribeTo(JobEvent.class, this);
    }

    @Override
    public void handle(String tenant, JobEvent message) {
        if (JobEventType.FAILED == message.getJobEventType()) {
            JobInfo job = jobService.retrieveJob(message.getJobId());
            if (JOB_TYPES.contains(job.getClassName())) {
                Type type = new TypeToken<Set<Long>>() {

                }.getType();
                List<Long> requestIds = job.getParametersAsMap().get(AbstractFeatureJob.IDS_PARAMETER).getValue(type);
                Set<Long> ids = Sets.newHashSet(requestIds);
                LOGGER.error("Job {} failed detected. Updating associated {} requests to ERROR status", job.getId().toString(), ids.size());
                featureRequestService.updateRequestsStatus(ids, RequestState.ERROR);
            }
        }
    }
}
