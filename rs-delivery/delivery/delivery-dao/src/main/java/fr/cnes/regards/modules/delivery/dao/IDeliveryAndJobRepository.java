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
package fr.cnes.regards.modules.delivery.dao;

import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryAndJob;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
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

    @Query("SELECT dj.deliveryRequest FROM DeliveryAndJob dj WHERE dj.jobInfo.id = :jobId ")
    Optional<DeliveryRequest> findDeliveryRequestByJobId(@Param("jobId") UUID jobInfoId);

    @Query("SELECT dj.jobInfo FROM DeliveryAndJob dj WHERE dj.deliveryRequest.id IN (:ids) AND dj.jobInfo.status"
           + ".status = :status")
    Page<JobInfo> findJobInfoByDeliveryRequestIdsAndStatus(@Param("ids") Collection<Long> deliveryRequestIds,
                                                           @Param("status") JobStatus jobStatus,
                                                           Pageable pageable);

    boolean existsByDeliveryRequestIdAndJobInfoClassName(Long deliveryRequestId, String jobClassName);

    // ------------
    // -- DELETE --
    // ------------

    void deleteByDeliveryRequestId(Long deliveryId);

    @Modifying
    @Query(value = "DELETE FROM DeliveryAndJob dj WHERE dj.deliveryRequest.id IN (:ids)")
    void deleteByDeliveryRequestIdIn(@Param("ids") Collection<Long> requestsToDeleteIds);

}
