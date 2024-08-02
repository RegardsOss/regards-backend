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
package fr.cnes.regards.modules.delivery.service.submission.update;

import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.domain.event.StopJobEvent;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.delivery.service.submission.DeliveryAndJobService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Handle jobs that are linked to expired {@link fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest}s.
 *
 * @author Iliana Ghazali
 **/
@Service
public class StopDeliveryExpiredJobsService {

    private final DeliveryAndJobService deliveryAndJobService;

    private final IJobInfoService jobInfoService;

    private final int expiredJobsPageSize;

    public StopDeliveryExpiredJobsService(DeliveryAndJobService deliveryAndJobService,
                                          IJobInfoService jobInfoService,
                                          @Value("${regards.delivery.request.expired.jobs.bulk.size:100}")
                                          int expiredJobsPageSize) {
        this.deliveryAndJobService = deliveryAndJobService;
        this.jobInfoService = jobInfoService;
        this.expiredJobsPageSize = expiredJobsPageSize;
    }

    /**
     * Send {@link StopJobEvent}s if jobs linked to expired
     * {@link fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest}s are still running.
     * These events will then be handled by another service to stop the jobs properly.
     *
     * @param expiredDeliveryRequestIds identifiers of expired delivery requests
     */
    public int handleExpiredRequestsRunningJobs(List<Long> expiredDeliveryRequestIds) {
        PageRequest pageableJobs = PageRequest.of(0, expiredJobsPageSize, Sort.by("id"));
        int nbExpiredJobs = 0;
        boolean hasNext;
        do {
            Page<JobInfo> jobsToStopPage = deliveryAndJobService.findJobInfoByDeliveryRequestIdsAndStatus(
                expiredDeliveryRequestIds,
                JobStatus.RUNNING,
                pageableJobs);
            if (jobsToStopPage.hasContent()) {
                nbExpiredJobs += jobInfoService.stopJobs(jobsToStopPage.getContent());
            }
            hasNext = jobsToStopPage.hasNext();
            if (hasNext) {
                pageableJobs = pageableJobs.next();
            }
        } while (hasNext);
        return nbExpiredJobs;
    }
}
