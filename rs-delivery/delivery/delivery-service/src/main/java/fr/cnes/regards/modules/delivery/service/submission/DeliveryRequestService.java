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
package fr.cnes.regards.modules.delivery.service.submission;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.delivery.dao.IDeliveryRequestRepository;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryErrorType;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Service to handle a {@link DeliveryRequest}.
 *
 * @author Iliana Ghazali
 **/
@MultitenantTransactional
@Service
public class DeliveryRequestService {

    private final IDeliveryRequestRepository deliveryRequestRepository;

    public DeliveryRequestService(IDeliveryRequestRepository deliveryRequestRepository) {
        this.deliveryRequestRepository = deliveryRequestRepository;
    }

    // ------------
    // -- SEARCH --
    // ------------

    public Page<Long> findExpiredDeliveryRequest(OffsetDateTime limitExpiryDate, Pageable pageable) {
        return deliveryRequestRepository.findByDeliveryStatusExpiryDateBefore(limitExpiryDate, pageable);
    }

    // ------------
    // -- UPDATE --
    // ------------

    public DeliveryRequest saveRequest(DeliveryRequest requestToSave) {
        return deliveryRequestRepository.save(requestToSave);
    }

    public List<DeliveryRequest> saveAllRequests(Collection<DeliveryRequest> requestsToSave) {
        return deliveryRequestRepository.saveAll(requestsToSave);
    }

    public void updateExpiredRequests(List<Long> requestIdsToUpdate, OffsetDateTime expiredDate) {
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

    public void deleteRequest(DeliveryRequest deliveryRequestToDelete) {
        deliveryRequestRepository.delete(deliveryRequestToDelete);
    }

}
