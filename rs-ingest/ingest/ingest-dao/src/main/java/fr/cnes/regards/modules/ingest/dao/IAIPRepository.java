/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;

/**
 * JPA Repository to access {@link AIPEntity}
 * @author Sébastien Binda
 *
 */
public interface IAIPRepository extends JpaRepository<AIPEntity, Long> {

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
     * Retrieve a page of {@link AIPEntity} matching the provided specification
     * @param aipEntitySpecification
     * @param pageable
     * @return a page of {@link AIPEntity}
     */
    Page<AIPEntity> findAll(Specification<AIPEntity> aipEntitySpecification, Pageable pageable);

    /**
     * Retrieve a list of aips thanks to their aipId
     * @param aipIds
     */
    Set<AIPEntity> findByAipIdIn(Collection<String> aipIds);

    /**
     * For testing purpose only
     */
    long countByState(AIPState sipState);

}
