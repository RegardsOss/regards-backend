/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryErrorType;
import fr.cnes.regards.modules.delivery.dto.output.DeliveryRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;

/**
 * Repository for {@link DeliveryRequest}.
 *
 * @author Iliana Ghazali
 **/
@Repository
public interface IDeliveryRequestRepository extends JpaRepository<DeliveryRequest, Long> {

    // ------------
    // -- SEARCH --
    // ------------
    @Query("SELECT req.id FROM DeliveryRequest req WHERE req.deliveryStatus.expiryDate <= :limitDate")
    Page<Long> findByDeliveryStatusExpiryDateBefore(@Param("limitDate") OffsetDateTime limitExpirationDate,
                                                    Pageable pageable);

    Page<DeliveryRequest> findDeliveryRequestByDeliveryStatusStatusIn(Collection<DeliveryRequestStatus> deliveryRequestStatuses,
                                                                      Pageable pageable);

    // ------------
    // -- UPDATE --
    // ------------

    @Modifying
    @Query(value = "UPDATE DeliveryRequest req "
                   + "SET req.deliveryStatus.status = :updateStatus, "
                   + "    req.deliveryStatus.errorCause = :updateMessage, "
                   + "    req.deliveryStatus.errorType = :updateType "
                   + "WHERE req.id IN (:ids)")
    void updateByIdIn(@Param("ids") Collection<Long> requestIdsToUpdate,
                      @Param("updateStatus") DeliveryRequestStatus updateRequestStatus,
                      @Param("updateType") DeliveryErrorType updateErrorType,
                      @Param("updateMessage") String updateMessage);

    // ------------
    // -- DELETE --
    // ------------

    @Modifying
    @Query("DELETE FROM DeliveryRequest req WHERE req.id IN (:ids)")
    void deleteAllByIdsIn(@Param("ids") Collection<Long> requestsToDeleteIds);
}
