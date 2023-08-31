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
package fr.cnes.regards.modules.delivery.service.submission.update;

import fr.cnes.regards.modules.delivery.service.submission.DeliveryRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Service to handle expired {@link fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest}s and eventually
 * their associated jobs that are still running.
 *
 * @author Iliana Ghazali
 **/
@Service
public class UpdateExpiredService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateExpiredService.class);

    private final DeliveryRequestService deliveryRequestService;

    private final StopDeliveryExpiredJobsService abortJobsService;

    private final int expiredRequestsPageSize;

    public UpdateExpiredService(DeliveryRequestService deliveryRequestService,
                                StopDeliveryExpiredJobsService abortJobsService,
                                @Value("${regards.delivery.request.expired.bulk.size:100}")
                                int expiredRequestsPageSize) {
        this.deliveryRequestService = deliveryRequestService;
        this.abortJobsService = abortJobsService;
        this.expiredRequestsPageSize = expiredRequestsPageSize;
    }

    /**
     * Handle all {@link fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest}s that have an expiration date
     * before the start of the task. Abort eventually jobs associated to expired requests that are still running.
     * This method is not annotated transactional because it calls transactional methods to handle actions
     * by page.
     *
     * @param limitExpirationDate current date to handle expirations
     */
    public void handleExpiredRequests(OffsetDateTime limitExpirationDate) {
        LOGGER.debug("Starting to find expired delivery requests after '{}'.", limitExpirationDate);
        int nbExpiredRequestsUpdated = 0;
        int nbLinkedRunningExpiredJobs = 0;
        PageRequest pageableRequests = PageRequest.of(0, expiredRequestsPageSize, Sort.by("id"));
        boolean hasNext;
        do {
            // search expired requests
            Page<Long> pageExpiredRequestIds = deliveryRequestService.findExpiredDeliveryRequest(limitExpirationDate,
                                                                                                 pageableRequests);
            if (pageExpiredRequestIds.hasContent()) {
                List<Long> expiredRequestIds = pageExpiredRequestIds.getContent();
                // update expired requests
                deliveryRequestService.updateExpiredRequests(expiredRequestIds, limitExpirationDate);
                // update eventually linked job that are still running
                nbLinkedRunningExpiredJobs += abortJobsService.handleExpiredRequestsRunningJobs(expiredRequestIds);
                nbExpiredRequestsUpdated += expiredRequestIds.size();
            }
            hasNext = pageExpiredRequestIds.hasNext();
            if (hasNext) {
                pageableRequests = pageableRequests.next();
            }
        } while (hasNext);

        LOGGER.debug("Updated {} expired delivery requests and {} associated running jobs after '{}'.",
                     nbExpiredRequestsUpdated,
                     nbLinkedRunningExpiredJobs,
                     limitExpirationDate);
    }

}
