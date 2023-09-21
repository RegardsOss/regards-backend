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

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.delivery.amqp.output.DeliveryResponseDtoEvent;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryAndJob;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryRequestStatus;
import fr.cnes.regards.modules.delivery.service.order.clean.job.CleanOrderJob;
import fr.cnes.regards.modules.delivery.service.order.zip.job.OrderDeliveryZipJob;
import fr.cnes.regards.modules.delivery.service.submission.DeliveryAndJobService;
import fr.cnes.regards.modules.delivery.service.submission.DeliveryRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Perform final tasks for finished {@link DeliveryRequest}s.
 *
 * @author Iliana Ghazali
 **/
@Service
public class EndingDeliveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndingDeliveryService.class);

    private final int finishedRequestsPageSize;

    private final DeliveryRequestService deliveryRequestService;

    private final DeliveryAndJobService deliveryAndJobService;

    private final IJobInfoService jobInfoService;

    private final IPublisher publisher;

    public EndingDeliveryService(DeliveryRequestService deliveryRequestService,
                                 DeliveryAndJobService deliveryAndJobService,
                                 IJobInfoService jobInfoService,
                                 IPublisher publisher,
                                 @Value("${regards.delivery.request.finished.bulk.size:100}")
                                 int finishedRequestsPageSize) {
        this.deliveryRequestService = deliveryRequestService;
        this.deliveryAndJobService = deliveryAndJobService;
        this.publisher = publisher;
        this.jobInfoService = jobInfoService;
        this.finishedRequestsPageSize = finishedRequestsPageSize;
    }

    /**
     * Schedule final tasks for finished {@link fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest}s, i.e.,
     * in {@link DeliveryRequestStatus#ERROR} or {@link DeliveryRequestStatus#DONE} states, by page.
     * Tasks:
     * <ul>
     *     <li>for DeliveryRequestStatus#DONE:
     *       <ul>
     *         <li>Schedule {@link OrderDeliveryZipJob} to make the order.</li>
     *       </ul>
     *     </li>
     *     <li>for DeliveryRequestStatus#ERROR:
     *       <ul>
     *         <li>Schedule {@link CleanOrderJob} to perform actions related to this state.</li>
     *         <li>Send a {@link DeliveryResponseDtoEvent} to notify this state.</li>
     *         <li>Finally, delete the corresponding {@link DeliveryRequest} in error (no retry is allowed).</li>
     *       </ul>
     *     </li>
     * </ul>
     */
    public void handleFinishedDeliveryRequests() {
        LOGGER.debug("Starting to find finished delivery requests");
        PageRequest pageableRequests = PageRequest.of(0, finishedRequestsPageSize, Sort.by("id"));
        int totalNbErrorRequests = 0;
        int totalNbDoneRequests = 0;
        boolean hasNext = false;
        do {
            // search expired requests
            Page<DeliveryRequest> pageFinishedRequests = deliveryRequestService.findDeliveryRequestByStatus(List.of(
                DeliveryRequestStatus.ERROR,
                DeliveryRequestStatus.DONE), pageableRequests);
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
                    this.handleErrorRequests(errorRequests);
                    totalNbErrorRequests += errorRequests.size();
                }
                // DONE requests
                List<DeliveryRequest> doneRequests = finishedRequests.get(DeliveryRequestStatus.DONE);
                if (!CollectionUtils.isEmpty(doneRequests)) {
                    this.handleDoneRequests(doneRequests);
                    totalNbDoneRequests += doneRequests.size();
                }

                // iterate on next page
                hasNext = pageFinishedRequests.hasNext();
                if (hasNext) {
                    pageableRequests = pageableRequests.next();
                }
            }
        } while (hasNext);

        LOGGER.debug("Handled {} ERROR delivery requests and {} DONE delivery requests.",
                     totalNbErrorRequests,
                     totalNbDoneRequests);
    }

    private void handleErrorRequests(List<DeliveryRequest> requestsInError) {
        int nbRequestsInError = requestsInError.size();
        List<Long> requestIds = new ArrayList<>(nbRequestsInError);
        List<String> correlationIds = new ArrayList<>(nbRequestsInError);
        List<DeliveryResponseDtoEvent> deliveryResponseEvents = new ArrayList<>(nbRequestsInError);

        for (DeliveryRequest request : requestsInError) {
            requestIds.add(request.getId());
            correlationIds.add(request.getCorrelationId());
            deliveryResponseEvents.add(buildDeliveryErrorResponse(request));
        }

        // schedule a clean job to handle properly ending of requests in error
        jobInfoService.createAsQueued(new JobInfo(false,
                                                  DeliveryJobPriority.CLEAN_ORDER_JOB_PRIORITY,
                                                  Set.of(new JobParameter(CleanOrderJob.CORRELATION_IDS,
                                                                          correlationIds)),
                                                  null,
                                                  CleanOrderJob.class.getName()));
        // publish response events
        publisher.publish(deliveryResponseEvents);
        // delete error requests
        deliveryAndJobService.deleteByDeliveryRequestIdIn(requestIds);
        deliveryRequestService.deleteRequests(requestIds);
    }

    private DeliveryResponseDtoEvent buildDeliveryErrorResponse(DeliveryRequest deliveryRequest) {
        return new DeliveryResponseDtoEvent(deliveryRequest.getCorrelationId(),
                                            deliveryRequest.getStatus(),
                                            deliveryRequest.getErrorType(),
                                            deliveryRequest.getErrorCause(),
                                            null,
                                            null,
                                            deliveryRequest.getOriginRequestAppId(),
                                            deliveryRequest.getOriginRequestPriority());
    }

    private void handleDoneRequests(List<DeliveryRequest> requestsInSuccess) {
        int nbRequestsInSuccess = requestsInSuccess.size();
        List<JobInfo> jobsInfo = new ArrayList<>(nbRequestsInSuccess);
        List<DeliveryAndJob> deliveryAndJobLinks = new ArrayList<>(nbRequestsInSuccess);
        for (DeliveryRequest request : requestsInSuccess) {
            JobInfo jobInfo = new JobInfo(false,
                                          DeliveryJobPriority.ORDER_DELIVERY_ZIP_JOB_PRIORITY,
                                          Set.of(),
                                          null,
                                          OrderDeliveryZipJob.class.getName());
            jobsInfo.add(jobInfo);
            deliveryAndJobLinks.add(new DeliveryAndJob(request, jobInfo));
        }
        jobInfoService.createAsQueued(jobsInfo);
        deliveryAndJobService.saveAllRequests(deliveryAndJobLinks);
    }

}
