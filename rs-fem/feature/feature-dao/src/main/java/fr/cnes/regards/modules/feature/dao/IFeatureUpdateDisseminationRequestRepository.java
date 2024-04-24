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
package fr.cnes.regards.modules.feature.dao;

import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureUpdateRequest;
import fr.cnes.regards.modules.feature.domain.request.dissemination.FeatureUpdateDisseminationInfoType;
import fr.cnes.regards.modules.feature.domain.request.dissemination.FeatureUpdateDisseminationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;

/**
 * JPA Repository to handle access to {@link FeatureUpdateDisseminationRequest} entities.
 *
 * @author Léo Mieulet
 */
public interface IFeatureUpdateDisseminationRequestRepository
    extends JpaRepository<FeatureUpdateDisseminationRequest, Long> {

    /**
     * @return a page of {@link FeatureUpdateDisseminationRequest} that updates {@link FeatureEntity} that
     * are not concerned by an existing {@link FeatureUpdateRequest} request (except request in error)
     */
    @Query(value = """
        SELECT fud FROM FeatureUpdateDisseminationRequest fud
         WHERE fud.updateType = :type
         AND fud.urn NOT IN
            (SELECT DISTINCT ur.urn
             FROM FeatureUpdateRequest ur
             WHERE ur.state != 'ERROR')
         AND fud.creationDate <= :now
         ORDER BY fud.creationDate""", countQuery = """
        SELECT count(fud.id) FROM FeatureUpdateDisseminationRequest fud
         WHERE fud.updateType = :type
         AND fud.urn NOT IN
            (SELECT DISTINCT ur.urn
             FROM FeatureUpdateRequest ur
             WHERE NOT ur.state != 'ERROR')
         AND fud.creationDate <= :now""")
    Page<FeatureUpdateDisseminationRequest> getFeatureUpdateDisseminationRequestsProcessable(
        @Param("now") OffsetDateTime now, @Param("type") FeatureUpdateDisseminationInfoType type, Pageable pageable);
}
