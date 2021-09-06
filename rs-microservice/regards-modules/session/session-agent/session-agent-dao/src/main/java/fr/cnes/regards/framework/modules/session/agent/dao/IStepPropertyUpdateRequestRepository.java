/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.session.agent.dao;

import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository for {@link StepPropertyUpdateRequest}
 *
 * @author Iliana Ghazali
 **/
@Repository
public interface IStepPropertyUpdateRequestRepository extends JpaRepository<StepPropertyUpdateRequest, Long> {


    Page<StepPropertyUpdateRequest> findBySourceAndRegistrationDateGreaterThanAndRegistrationDateLessThan(String source, OffsetDateTime lastUpdate,
            OffsetDateTime freezeDate, Pageable page);

    long countBySourceAndRegistrationDateGreaterThanAndRegistrationDateLessThan(String source, OffsetDateTime lastUpdate, OffsetDateTime freezeDate);

    Page<StepPropertyUpdateRequest> findBySourceAndRegistrationDateBefore(String source, OffsetDateTime freezeDate, Pageable page);

    long countBySourceAndRegistrationDateBefore(String source, OffsetDateTime lastUpdate);

    void deleteBySessionStepIn(List<SessionStep> sessionSteps);

    List<StepPropertyUpdateRequest> findBySession(String session);

    @Modifying
    @Query("DELETE FROM SnapshotProcess p where p.source NOT IN (SELECT s.source FROM StepPropertyUpdateRequest s) "
            + "AND (p.lastUpdateDate IS NULL OR p.lastUpdateDate <= ?1)")
    int deleteUnusedProcess(OffsetDateTime limitDate);

    // For tests
    Page<StepPropertyUpdateRequest> findBySourceAndCreationDateGreaterThanAndCreationDateLessThan(String source,
            OffsetDateTime lastUpdate, OffsetDateTime freezeDate, Pageable page);

}
