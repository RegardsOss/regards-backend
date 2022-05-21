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
package fr.cnes.regards.framework.modules.session.commons.dao;

import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * JPA Repository for {@link SessionStep}
 *
 * @author Iliana Ghazali
 **/

@Repository
public interface ISessionStepRepository extends JpaRepository<SessionStep, Long> {

    Optional<SessionStep> findBySourceAndSessionAndStepId(String source, String session, String stepId);

    /**
     * Methods to calculate snapshots of SessionSteps
     */
    int countBySourceAndRegistrationDateBefore(String source, OffsetDateTime schedulerStartDate);

    Page<SessionStep> findBySourceAndRegistrationDateBefore(String source,
                                                            OffsetDateTime freezeDate,
                                                            Pageable pageToRequest);

    int countBySourceAndRegistrationDateGreaterThanAndRegistrationDateLessThan(String source,
                                                                               OffsetDateTime lastUpdateDate,
                                                                               OffsetDateTime schedulerStartDate);

    Page<SessionStep> findBySourceAndRegistrationDateGreaterThanAndRegistrationDateLessThan(String source,
                                                                                            OffsetDateTime lastUpdateDate,
                                                                                            OffsetDateTime freezeDate,
                                                                                            Pageable pageToRequest);

    /**
     * Clean SessionSteps
     */
    void deleteByLastUpdateDateBefore(OffsetDateTime startClean);

    Page<SessionStep> findByLastUpdateDateBefore(OffsetDateTime startClean, Pageable page);

    /**
     * Return all names of sources and sessions associated to SessionSteps
     */
    Page<ISessionStepLight> findBy(Pageable pageToRequest);

}
