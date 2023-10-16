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
package fr.cnes.regards.modules.delivery.service.order.manager;

import fr.cnes.regards.framework.jpa.multitenant.lock.LockServiceTask;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryRequestStatus;
import fr.cnes.regards.modules.delivery.service.schedulers.UpdateExpiredDeliveryRequestScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Task linked to {@link UpdateExpiredDeliveryRequestScheduler} to handle
 * expired {@link fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest}s.
 *
 * @author Iliana Ghazali
 **/
public class EndingDeliveryTask implements LockServiceTask<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndingDeliveryTask.class);

    private final EndingDeliveryService endingDeliveryService;

    private final int finishedRequestsPageSize;

    public EndingDeliveryTask(EndingDeliveryService endingDeliveryService, int finishedRequestsPageSize) {
        this.endingDeliveryService = endingDeliveryService;
        this.finishedRequestsPageSize = finishedRequestsPageSize;
    }

    /**
     * Schedule final tasks for finished {@link fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest}s, i.e.,
     * in {@link DeliveryRequestStatus#ERROR} or {@link DeliveryRequestStatus#DONE} states, by page.
     */
    @Override
    public Void run() {
        long start = System.currentTimeMillis();
        LOGGER.debug("Starting ending delivery task.");

        PageRequest pageableRequests = PageRequest.of(0, finishedRequestsPageSize, Sort.by("id"));
        int totalNbErrorRequests = 0;
        int totalNbDoneRequests = 0;
        boolean hasNext = false;
        do {
            // search expired requests
            Page<DeliveryRequest> pageFinishedRequests = endingDeliveryService.findDeliveryRequestsToProcess(
                pageableRequests);
            if (pageFinishedRequests.hasContent()) {
                Map<DeliveryRequestStatus, List<DeliveryRequest>> finishedRequests = pageFinishedRequests.getContent()
                                                                                                         .stream()
                                                                                                         .collect(
                                                                                                             Collectors.groupingBy(
                                                                                                                 DeliveryRequest::getStatus));
                // handle requests according to their status
                // ERROR requests
                List<DeliveryRequest> errorRequests = finishedRequests.get(DeliveryRequestStatus.ERROR);
                if (!CollectionUtils.isEmpty(errorRequests)) {
                    endingDeliveryService.handleErrorRequests(errorRequests);
                    totalNbErrorRequests += errorRequests.size();
                }
                // DONE requests
                List<DeliveryRequest> doneRequests = finishedRequests.get(DeliveryRequestStatus.DONE);
                if (!CollectionUtils.isEmpty(doneRequests)) {
                    endingDeliveryService.handleDoneRequests(doneRequests);
                    totalNbDoneRequests += doneRequests.size();
                }

                // iterate on next page
                hasNext = pageFinishedRequests.hasNext();
                if (hasNext) {
                    pageableRequests = pageableRequests.next();
                }
            }
        } while (hasNext);

        LOGGER.debug("End of update expired task. Handled {} ERROR delivery requests and {} DONE delivery requests. "
                     + "Took {}ms.", totalNbErrorRequests, totalNbDoneRequests, System.currentTimeMillis() - start);
        return null;
    }

}
