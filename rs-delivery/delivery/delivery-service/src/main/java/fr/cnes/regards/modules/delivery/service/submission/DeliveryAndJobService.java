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
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.modules.delivery.dao.IDeliveryAndJobRepository;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryAndJob;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service to handle a {@link DeliveryAndJob} entity.
 *
 * @author Iliana Ghazali
 **/
@Service
public class DeliveryAndJobService {

    private final IDeliveryAndJobRepository deliveryAndJobRepository;

    public DeliveryAndJobService(IDeliveryAndJobRepository deliveryAndJobRepository) {
        this.deliveryAndJobRepository = deliveryAndJobRepository;
    }

    // ------------
    // -- SEARCH --
    // ------------

    @MultitenantTransactional(readOnly = true)
    public Optional<DeliveryRequest> findDeliveryRequestByJobId(UUID jobId) {
        return deliveryAndJobRepository.findDeliveryRequestByJobId(jobId);
    }

    @MultitenantTransactional(readOnly = true)
    public Page<JobInfo> findJobInfoByDeliveryRequestIdsAndStatus(Collection<Long> deliveryRequestIds,
                                                                  JobStatus status,
                                                                  Pageable pageable) {
        return deliveryAndJobRepository.findJobInfoByDeliveryRequestIdsAndStatus(deliveryRequestIds, status, pageable);
    }

    // ------------
    // -- UPDATE --
    // ------------

    @MultitenantTransactional
    public List<DeliveryAndJob> saveAllRequests(Collection<DeliveryAndJob> requestsToSave) {
        return deliveryAndJobRepository.saveAll(requestsToSave);
    }

    // ------------
    // -- DELETE --
    // ------------

    @MultitenantTransactional
    public void deleteByDeliveryRequestId(Long deliveryRequestId) {
        deliveryAndJobRepository.deleteByDeliveryRequestId(deliveryRequestId);
    }

    @MultitenantTransactional
    public void deleteByDeliveryRequestIdIn(Collection<Long> deliveryRequestIds) {
        deliveryAndJobRepository.deleteByDeliveryRequestIdIn(deliveryRequestIds);
    }

}
