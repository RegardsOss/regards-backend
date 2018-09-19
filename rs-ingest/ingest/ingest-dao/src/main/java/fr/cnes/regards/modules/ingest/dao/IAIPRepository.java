/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.dao;

import java.util.Optional;
import java.util.Set;

import javax.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import fr.cnes.regards.modules.ingest.domain.entity.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SipAIPState;
import fr.cnes.regards.modules.storage.domain.IAipState;

/**
 * JPA Repository to access {@link AIPEntity}
 * @author Sébastien Binda
 *
 */
public interface IAIPRepository extends JpaRepository<AIPEntity, Long> {

    /**
     * Retrieve all {@link AIPEntity}s associated to the given {@link SIPEntity}
     * @param sip {@link SIPEntity}
     * @return {@link AIPEntity}s
     */
    Set<AIPEntity> findBySip(SIPEntity sip);

    /**
     * Retrieve all {@link AIPEntity}s associated to the given {@link SIPEntity}
     * @param sipId SIP identifier
     * @return {@link AIPEntity}s
     */
    Set<AIPEntity> findBySipSipId(String sipId);

    /**
     * Retrieve an {@link AIPEntity} by its {@link AIPEntity#getAipId()}
     * @param aipId SIP identifier
     * @return optional {@link AIPEntity}
     */
    Optional<AIPEntity> findByAipId(String aipId);

    /**
     * Retrieve an {@link AIPEntity} by is {@link AIPEntity#getState()}
     * @param state {@link SipAIPState}
     * @return optional {@link AIPEntity}
     */
    @Query("select id from AIPEntity a where a.state= ?1")
    Set<Long> findIdByState(IAipState state);

    default boolean isAlreadyWorking(String processingChain) {
        Page<AIPEntity> page = findWithLockBySipProcessingAndState(processingChain, SipAIPState.SUBMISSION_SCHEDULED,
                                                                   new PageRequest(0, 1));
        return page.hasContent();
    }

    @Lock(LockModeType.PESSIMISTIC_READ)
    Page<AIPEntity> findWithLockBySipProcessingAndState(String processingChain, IAipState state, Pageable pageable);

    Set<AIPEntity> findBySipProcessingAndState(String processingChain, IAipState state);

    /**
     * Update state of the given {@link AIPEntity}
     * @param state New state
     * @param id {@link AIPEntity} to update
     */
    @Modifying
    @Query("UPDATE AIPEntity a set a.state = ?1, a.errorMessage = ?3 where a.aipId = ?2")
    void updateAIPEntityStateAndErrorMessage(IAipState state, String aipId, String errorMessage);

    /**
     * Switch state for a given session
     */
    @Modifying
    @Query(value = "UPDATE {h-schema}t_aip set state = ?1, error_message = ?2 FROM {h-schema}t_sip sip WHERE t_aip.sip_id = sip.id AND t_aip.state = ?3 AND sip.session = ?4",
            nativeQuery = true)
    void updateAIPEntityStateAndErrorMessageByStateAndSessionId(String state, String errorMessage, String filterState,
            String sessionId);
}
