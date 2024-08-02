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
package fr.cnes.regards.modules.feature.dao;

import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureUpdateRequest;
import fr.cnes.regards.modules.feature.domain.request.dissemination.FeatureUpdateDisseminationInfoType;
import fr.cnes.regards.modules.feature.domain.request.dissemination.FeatureUpdateDisseminationRequest;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * JPA Repository to handle access to {@link FeatureUpdateDisseminationRequest} entities.
 *
 * @author Léo Mieulet
 */
public interface IFeatureUpdateDisseminationRequestRepository
    extends JpaRepository<FeatureUpdateDisseminationRequest, Long> {

    /**
     * @return a page of {@link FeatureUpdateDisseminationRequest} that updates {@link FeatureEntity} that
     * are not concerned by an existing {@link FeatureUpdateRequest} running request
     */
    default Page<FeatureUpdateDisseminationRequest> getFeatureUpdateDisseminationRequestsProcessable(OffsetDateTime now,
                                                                                                     FeatureUpdateDisseminationInfoType type,
                                                                                                     Pageable pageable) {
        return doGetFeatureUpdateDisseminationRequestsProcessable(now,
                                                                  type.ordinal(),
                                                                  Arrays.stream(FeatureRequestStep.values())
                                                                        .filter(FeatureRequestStep::isProcessing)
                                                                        .map(Enum::name)
                                                                        .toList(),
                                                                  RequestState.GRANTED.name(),
                                                                  pageable);
    }

    @Query(value = """
        SELECT id as id, 
               feature_urn as feature_urn,
               recipient_label as recipient_label,
               creation_date as creation_date,
               update_type as update_type,
               ack_required as ack_required,
               blocking_required as blocking_required
        FROM t_feature_update_dissemination fud
        WHERE fud.update_type = :type
        AND NOT EXISTS(
             SELECT 1 FROM t_feature_request req
             WHERE req.request_type = 'UPDATE'
             AND req.state = :state
             AND req.step in (:running_steps)
             AND fud.feature_urn = req.urn
             LIMIT 1
             )
        AND fud.creation_date <= :now
        ORDER BY fud.creation_date""", countQuery = """
        SELECT count(fud.id) FROM t_feature_update_dissemination fud
         WHERE fud.update_type = :type
         AND NOT EXISTS(
             SELECT 1 FROM t_feature_request req
             WHERE
             req.request_type = 'UPDATE'
             AND req.state = :state
             AND req.step in (:running_steps)
             AND fud.feature_urn = req.urn
             LIMIT 1)
         AND fud.creation_date <= :now""", nativeQuery = true)
    Page<FeatureUpdateDisseminationRequest> doGetFeatureUpdateDisseminationRequestsProcessable(
        @Param("now") OffsetDateTime now,
        @Param("type") int type,
        @Param("running_steps") List<String> runningSteps,
        @Param("state") String state,
        Pageable pageable);
}
