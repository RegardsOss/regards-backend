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
package fr.cnes.regards.modules.delivery.service.submission;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.delivery.dao.IDeliveryRequestRepository;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryErrorType;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Service to handle a {@link DeliveryRequest}.
 *
 * @author Iliana Ghazali
 **/
@Service
public class DeliveryRequestService {

    private final IDeliveryRequestRepository deliveryRequestRepository;

    public DeliveryRequestService(IDeliveryRequestRepository deliveryRequestRepository) {
        this.deliveryRequestRepository = deliveryRequestRepository;
    }

    // ------------
    // -- SEARCH --
    // ------------

    @MultitenantTransactional(readOnly = true)
    public Optional<DeliveryRequest> findDeliveryRequest(long deliveryRequestId) {
        return deliveryRequestRepository.findById(deliveryRequestId);
    }

    @MultitenantTransactional(readOnly = true)
    public Page<Long> findExpiredDeliveryRequest(OffsetDateTime limitExpiryDate, Pageable pageable) {
        return deliveryRequestRepository.findByDeliveryStatusExpiryDateBefore(limitExpiryDate, pageable);
    }

    @MultitenantTransactional(readOnly = true)
    public Page<DeliveryRequest> findDeliveryRequestByStatus(Collection<DeliveryRequestStatus> finishedRequests,
                                                             Pageable pageable) {
        return deliveryRequestRepository.findDeliveryRequestByDeliveryStatusStatusIn(finishedRequests, pageable);
    }

    @MultitenantTransactional(readOnly = true)
    public List<DeliveryRequest> findDeliveryRequestByCorrelationIds(Collection<String> correlationIds) {
        return deliveryRequestRepository.findByCorrelationIdIn(correlationIds);
    }

    // ------------
    // -- UPDATE --
    // ------------

    @MultitenantTransactional
    public DeliveryRequest saveRequest(DeliveryRequest requestToSave) {
        return deliveryRequestRepository.save(requestToSave);
    }

    @MultitenantTransactional
    public List<DeliveryRequest> saveAllRequests(Collection<DeliveryRequest> requestsToSave) {
        return deliveryRequestRepository.saveAll(requestsToSave);
    }

    /**
     * Update the {@link DeliveryRequest} in database. Update the following information if the delivery request is not
     * in ERROR status :
     * <ul>
     * <li>order identifier</li>
     * <li>total number of sub-orders</li>
     * <li>status of the delivery request</li>
     * <li>error type if an error exists</li>
     * <li>error cause if an error exiists</li>
     * </ul>
     */
    @MultitenantTransactional(propagation = Propagation.REQUIRES_NEW)
    public void updateRequest(Long deliveryRequestId,
                              Long orderId,
                              Integer totalSubOrders,
                              DeliveryRequestStatus status,
                              DeliveryErrorType errorType,
                              String errorCause) {
        DeliveryRequest deliveryRequest = deliveryRequestRepository.findByIdOptimisticLock(deliveryRequestId);
        if (DeliveryRequestStatus.ERROR == deliveryRequest.getDeliveryStatus().getStatus()) {
            return;
        }
        deliveryRequest.update(orderId, totalSubOrders, status, errorType, errorCause);
        deliveryRequestRepository.save(deliveryRequest);
    }

    @MultitenantTransactional
    public void updateExpiredRequests(Collection<Long> requestIdsToUpdate, OffsetDateTime expiredDate) {
        deliveryRequestRepository.updateByIdIn(requestIdsToUpdate,
                                               DeliveryRequestStatus.ERROR,
                                               DeliveryErrorType.EXPIRED,
                                               String.format("The request"
                                                             + " date has reached the "
                                                             + "expiration limit '%s'", expiredDate));
    }

    // ------------
    // -- DELETE --
    // ------------
    @MultitenantTransactional
    public void deleteRequestById(Long deliveryRequestToDeleteId) {
        deliveryRequestRepository.deleteById(deliveryRequestToDeleteId);
    }

    @MultitenantTransactional
    public void deleteRequests(Collection<Long> deliveryRequestsToDeleteIds) {
        deliveryRequestRepository.deleteAllByIdsIn(deliveryRequestsToDeleteIds);
    }

}
