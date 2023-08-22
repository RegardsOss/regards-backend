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
package fr.cnes.regards.modules.delivery.dao;

import fr.cnes.regards.modules.delivery.domain.input.DeliveryAndJob;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link DeliveryAndJob}.
 *
 * @author Iliana Ghazali
 **/
public interface IDeliveryAndJobRepository extends JpaRepository<DeliveryAndJob, Long> {

    // ------------
    // -- SEARCH --
    // ------------

    @Query("select dj.deliveryRequest from DeliveryAndJob dj where dj.jobInfo.id = :jobId ")
    Optional<DeliveryRequest> findDeliveryRequestByJobId(@Param("jobId") UUID jobInfoId);

    // ------------
    // -- DELETE --
    // ------------

    void deleteByDeliveryRequestId(Long deliveryId);
}
