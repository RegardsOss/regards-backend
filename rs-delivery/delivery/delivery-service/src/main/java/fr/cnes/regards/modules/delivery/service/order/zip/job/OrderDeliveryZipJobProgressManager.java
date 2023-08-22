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
package fr.cnes.regards.modules.delivery.service.order.zip.job;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.delivery.amqp.output.DeliveryResponseDtoEvent;
import fr.cnes.regards.modules.delivery.domain.exception.DeliveryOrderException;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryStatus;
import fr.cnes.regards.modules.delivery.domain.zip.ZipDeliveryInfo;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryErrorType;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryRequestStatus;
import fr.cnes.regards.modules.delivery.service.submission.DeliveryAndJobService;
import fr.cnes.regards.modules.delivery.service.submission.DeliveryRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Handle the progress of a delivery request processing.
 * Success and failures outcomes are handled by updating the {@link DeliveryRequest} and publishing the
 * corresponding {@link DeliveryResponseDtoEvent}.
 *
 * @author Iliana Ghazali
 **/
@MultitenantTransactional
@Service
public class OrderDeliveryZipJobProgressManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderDeliveryZipJobProgressManager.class);

    private final DeliveryAndJobService deliveryAndJobService;

    private final DeliveryRequestService deliveryRequestService;

    private final IPublisher publisher;

    public OrderDeliveryZipJobProgressManager(DeliveryAndJobService deliveryAndJobService,
                                              DeliveryRequestService deliveryRequestService,
                                              IPublisher publisher) {
        this.deliveryAndJobService = deliveryAndJobService;
        this.deliveryRequestService = deliveryRequestService;
        this.publisher = publisher;
    }

    /**
     * Retrieve delivery request to process linked to the job id.
     *
     * @throws RsRuntimeException if delivery request could not be found
     */
    public DeliveryRequest findDeliveryRequestToProcess(UUID jobInfoId) {
        return deliveryAndJobService.findDeliveryRequestByJobId(jobInfoId)
                                    .orElseThrow(() -> new RsRuntimeException(String.format(
                                        "Could not find delivery request linked to job id %s.",
                                        jobInfoId)));
    }

    /**
     * Handle a successful delivery by publishing a success {@link DeliveryResponseDtoEvent} and deleting the
     * {@link DeliveryRequest} and its linked job in the association table.
     */
    public void handleDeliverySuccess(DeliveryRequest deliveryRequest, ZipDeliveryInfo zipUploadedInfo) {
        LOGGER.info("""
                        Successfully ended processing of delivery with correlation id '{}'.
                        Uploaded zip info : {}.
                        """, deliveryRequest.getCorrelationId(), zipUploadedInfo);
        publisher.publish(buildDeliverySuccessResponse(deliveryRequest, zipUploadedInfo));
        deliveryAndJobService.deleteByDeliveryRequestId(deliveryRequest.getId());
        deliveryRequestService.deleteRequest(deliveryRequest);
    }

    /**
     * Handle a delivery in error by updating the {@link DeliveryRequest} to an error status and publishing a
     * {@link DeliveryResponseDtoEvent}.
     */
    public void handleDeliveryError(DeliveryRequest deliveryRequest, DeliveryOrderException exception) {
        LOGGER.error("Delivery with correlation id '{}' ended in error.",
                     deliveryRequest.getCorrelationId(),
                     exception);
        updateDeliveryRequestInError(deliveryRequest, exception);
        publisher.publish(buildDeliveryErrorResponse(deliveryRequest, exception));
    }

    // ------------
    // HELPERS
    // ------------
    private DeliveryResponseDtoEvent buildDeliverySuccessResponse(DeliveryRequest deliveryRequest,
                                                                  ZipDeliveryInfo zipUploadedInfo) {
        return new DeliveryResponseDtoEvent(deliveryRequest.getCorrelationId(),
                                            DeliveryRequestStatus.DONE,
                                            null,
                                            null,
                                            zipUploadedInfo.uri(),
                                            zipUploadedInfo.md5Checksum(),
                                            deliveryRequest.getOriginRequestAppId(),
                                            deliveryRequest.getOriginRequestPriority());
    }

    private void updateDeliveryRequestInError(DeliveryRequest deliveryRequest, DeliveryOrderException exception) {
        DeliveryStatus deliveryStatus = deliveryRequest.getDeliveryStatus();
        deliveryStatus.setStatusDate(OffsetDateTime.now());
        deliveryStatus.setStatus(DeliveryRequestStatus.ERROR);
        deliveryStatus.setError(DeliveryErrorType.INTERNAL_ERROR, exception.getMessage());
        deliveryRequestService.saveRequest(deliveryRequest);
    }

    private DeliveryResponseDtoEvent buildDeliveryErrorResponse(DeliveryRequest deliveryRequest,
                                                                DeliveryOrderException exception) {
        return new DeliveryResponseDtoEvent(deliveryRequest.getCorrelationId(),
                                            DeliveryRequestStatus.ERROR,
                                            DeliveryErrorType.INTERNAL_ERROR,
                                            exception.getMessage(),
                                            null,
                                            null,
                                            deliveryRequest.getOriginRequestAppId(),
                                            deliveryRequest.getOriginRequestPriority());
    }

}
