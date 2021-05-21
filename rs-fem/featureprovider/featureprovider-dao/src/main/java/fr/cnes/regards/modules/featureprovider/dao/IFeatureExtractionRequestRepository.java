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
package fr.cnes.regards.modules.featureprovider.dao;

import fr.cnes.regards.modules.feature.domain.request.FeatureCreationRequest;
import fr.cnes.regards.modules.feature.domain.request.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.featureprovider.domain.FeatureExtractionRequest;
import fr.cnes.regards.modules.featureprovider.domain.IFeatureExtractionRequestLight;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Kevin Marchois
 *
 */
@Repository
public interface IFeatureExtractionRequestRepository extends JpaRepository<FeatureExtractionRequest, Long> {

    /**
     * Get a page of {@link FeatureExtractionRequest} with specified step.
     * @param now
     * @return a list of {@link FeatureCreationRequest}
     */
    @Query("select frr from FeatureExtractionRequest frr where frr.step = :localDelayed and frr.requestDate <= :now")
    List<FeatureExtractionRequest> findByStep(@Param("localDelayed") FeatureRequestStep localDelayed,
            @Param("now") OffsetDateTime now, Pageable page);

    Set<IFeatureExtractionRequestLight> findByRequestIdIn(@Param("requestIds") Set<String> requestIds);

    /**
     * Update {@link FeatureRequestStep} step
     * @param step new {@link FeatureRequestStep}
     * @param ids id of {@link FeatureExtractionRequest} to update
     */
    @Modifying
    @Query("update FeatureExtractionRequest frr set frr.step = :newStep where frr.id in :ids")
    void updateStep(@Param("newStep") FeatureRequestStep step, @Param("ids") Set<Long> ids);

    @Query("select distinct fcr.requestId from FeatureExtractionRequest fcr")
    Set<String> findRequestId();

    @Modifying
    @Query("update FeatureExtractionRequest frr set frr.step = :newStep where frr.requestId in :requestIds")
    void updateStepByRequestIdIn(@Param("newStep") FeatureRequestStep step,
            @Param("requestIds") Set<String> requestIds);

    @Modifying
    @Query("update FeatureExtractionRequest frr set frr.state = :newState where frr.requestId in :requestIds")
    void updateState(@Param("newState") RequestState state, @Param("requestIds") Set<String> requestIds);

    @Modifying(clearAutomatically = true)
    @Query("delete from FeatureExtractionRequest frr where frr.requestId in :requestIds")
    void deleteAllByRequestIdIn(@Param("requestIds") Set<String> requestIds);


    Page<FeatureExtractionRequest> findByMetadataSessionOwnerAndStateIn(String sessionOwner, Set<RequestState> states,
            Pageable pageToRequest);

    Page<FeatureExtractionRequest> findByMetadataSessionOwnerAndMetadataSessionAndStateIn(String sessionOwner, String session,
            Set<RequestState> states, Pageable pageToRequest);

}
