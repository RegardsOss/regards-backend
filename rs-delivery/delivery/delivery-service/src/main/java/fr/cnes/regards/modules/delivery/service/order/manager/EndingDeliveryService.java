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
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.delivery.amqp.output.DeliveryResponseDtoEvent;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryAndJob;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryRequestStatus;
import fr.cnes.regards.modules.delivery.service.order.clean.job.CleanDeliveryOrderJob;
import fr.cnes.regards.modules.delivery.service.order.zip.job.OrderDeliveryZipJob;
import fr.cnes.regards.modules.delivery.service.submission.DeliveryAndJobService;
import fr.cnes.regards.modules.delivery.service.submission.DeliveryRequestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Perform final tasks for finished {@link DeliveryRequest}s. <br/>
 * Tasks:
 * <ul>
 *     <li>for {@link DeliveryRequestStatus#DONE}:
 *       <ul>
 *         <li>Schedule {@link OrderDeliveryZipJob} to make the order.</li>
 *       </ul>
 *     </li>
 *     <li>for {@link DeliveryRequestStatus#ERROR}:
 *       <ul>
 *         <li>Schedule {@link CleanOrderJob} to perform actions related to this state.</li>
 *         <li>Send a {@link DeliveryResponseDtoEvent} to notify this state.</li>
 *         <li>Finally, delete the corresponding {@link DeliveryRequest} in error (no retry is allowed).</li>
 *       </ul>
 *     </li>
 * </ul>
 *
 * @author Iliana Ghazali
 **/
@Service
public class EndingDeliveryService {

    private final DeliveryRequestService deliveryRequestService;

    private final DeliveryAndJobService deliveryAndJobService;

    private final IJobInfoService jobInfoService;

    private final IPublisher publisher;

    public EndingDeliveryService(DeliveryRequestService deliveryRequestService,
                                 DeliveryAndJobService deliveryAndJobService,
                                 IJobInfoService jobInfoService,
                                 IPublisher publisher) {
        this.deliveryRequestService = deliveryRequestService;
        this.deliveryAndJobService = deliveryAndJobService;
        this.publisher = publisher;
        this.jobInfoService = jobInfoService;
    }

    public Page<DeliveryRequest> findDeliveryRequestsToProcess(PageRequest pageableRequests) {
        return deliveryRequestService.findDeliveryRequestByStatus(List.of(DeliveryRequestStatus.ERROR,
                                                                          DeliveryRequestStatus.DONE),
                                                                  pageableRequests);
    }

    /**
     * Handle {@link DeliveryRequest}s in {@link DeliveryRequestStatus#ERROR} status in a single transaction.
     */
    @MultitenantTransactional
    public void handleErrorRequests(List<DeliveryRequest> requestsInError) {
        int nbRequestsInError = requestsInError.size();
        List<Long> requestIds = new ArrayList<>(nbRequestsInError);
        List<DeliveryResponseDtoEvent> deliveryResponseEvents = new ArrayList<>(nbRequestsInError);

        for (DeliveryRequest request : requestsInError) {
            requestIds.add(request.getId());
            deliveryResponseEvents.add(buildDeliveryErrorResponse(request));
        }

        // schedule a clean job to handle properly ending of requests in error
        jobInfoService.createAsQueued(new JobInfo(false,
                                                  DeliveryJobPriority.CLEAN_ORDER_JOB_PRIORITY,
                                                  Set.of(new JobParameter(CleanDeliveryOrderJob.DELIVERY_REQUESTS_TO_CLEAN,
                                                                          requestsInError)),
                                                  null,
                                                  CleanDeliveryOrderJob.class.getName()));
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

    /**
     * Handle {@link DeliveryRequest}s in {@link DeliveryRequestStatus#DONE} status in a single transaction.
     */
    @MultitenantTransactional
    public void handleDoneRequests(List<DeliveryRequest> requestsInSuccess) {

        int nbRequestsInSuccess = requestsInSuccess.size();
        List<JobInfo> jobsInfo = new ArrayList<>(nbRequestsInSuccess);
        List<DeliveryAndJob> deliveryAndJobLinks = new ArrayList<>(nbRequestsInSuccess);
        for (DeliveryRequest request : requestsInSuccess) {
            // check if request is already associated to OrderDeliveryZipJob
            if (!deliveryAndJobService.existsByDeliveryRequestIdAndJobInfoClassName(request.getId(),
                                                                                    OrderDeliveryZipJob.class.getName())) {
                JobInfo jobInfo = new JobInfo(false,
                                              DeliveryJobPriority.ORDER_DELIVERY_ZIP_JOB_PRIORITY,
                                              Set.of(),
                                              null,
                                              OrderDeliveryZipJob.class.getName());
                jobsInfo.add(jobInfo);
                deliveryAndJobLinks.add(new DeliveryAndJob(request, jobInfo));
            }
        }
        jobInfoService.createAsQueued(jobsInfo);
        deliveryAndJobService.saveAllRequests(deliveryAndJobLinks);
    }

}
