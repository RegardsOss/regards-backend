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

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestStep;

/**
 * @author Léo Mieulet
 */
@Repository
public interface IAbstractRequestRepository extends JpaRepository<AbstractRequest, Long> {

    /**
     * Retrieve a page of {@link AbstractRequest} matching the provided specification
     * @param aipEntitySpecification criteria spec
     * @return a page of {@link AbstractRequest}
     */
    Page<AbstractRequest> findAll(Specification<AbstractRequest> aipEntitySpecification, Pageable pageable);

    /**
     * Retrieve a list of {@link AbstractRequest} referencing the provided specification
     * @param aipEntitySpecification criteria spec
     * @return a list of {@link AbstractRequest}
     */
    List<AbstractRequest> findAll(Specification<AbstractRequest> aipEntitySpecification);

    /**
     * Update the state of list of entities using their ids
     * @param ids request ids
     * @param state new state
     * @return
     */
    @Modifying
    @Query(value = "UPDATE AbstractRequest SET state = :state WHERE id IN (:ids)")
    int updateStates(@Param("ids") List<Long> ids, @Param("state") InternalRequestStep state);
}
