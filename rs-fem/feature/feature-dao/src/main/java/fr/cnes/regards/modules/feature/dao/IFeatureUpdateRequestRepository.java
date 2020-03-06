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
package fr.cnes.regards.modules.feature.dao;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.feature.domain.request.FeatureUpdateRequest;

/**
 *
 * @author Marc SORDI
 *
 */
@Repository
public interface IFeatureUpdateRequestRepository extends JpaRepository<FeatureUpdateRequest, Long> {

    /**
     * Retrieve update requests to process sorted by request date.<br/>
     * Sorting requests is useful to manage several update requests on a same target entity!
     */
    List<FeatureUpdateRequest> findAllByIdInOrderByRequestDateAsc(Set<Long> ids);

    @Query("select distinct fcr.requestId from FeatureUpdateRequest fcr")
    public Set<String> findRequestId();
}
