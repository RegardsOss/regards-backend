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

import fr.cnes.regards.modules.feature.domain.request.FeatureDeletionRequest;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Kevin Marchois
 */
@Repository
public interface IFeatureDeletionRequestRepository extends IAbstractFeatureRequestRepository<FeatureDeletionRequest> {

    Set<FeatureDeletionRequest> findByGroupIdIn(Set<String> groupId);

    @Query("SELECT fdr FROM FeatureDeletionRequest fdr WHERE fdr.step = :step and fdr.requestDate <= :now")
    Set<FeatureDeletionRequest> findByStep(@Param("step") FeatureRequestStep step,
                                           @Param("now") OffsetDateTime offsetDateTime);

    @Query("""
           SELECT fdr FROM FeatureDeletionRequest fdr 
           WHERE fdr.step IN (:steps) AND fdr.requestDate <= :now AND fdr.urn IN (:urns) 
        """)
    Set<FeatureDeletionRequest> findByUrnInAndStepIn(@Param("steps") Collection<FeatureRequestStep> steps,
                                                     @Param("urns") Collection<FeatureUniformResourceName> urns,
                                                     @Param("now") OffsetDateTime offsetDateTime);

    @Query("""
        SELECT fdr FROM FeatureDeletionRequest fdr WHERE fdr.step IN (:steps) AND fdr.requestDate <= :now
        """)
    Set<FeatureDeletionRequest> findByStepIn(@Param("steps") Collection<FeatureRequestStep> steps,
                                             @Param("now") OffsetDateTime offsetDateTime);

    /**
     * Get a limited number of {@link FeatureDeletionRequest} ready to be scheduled ordered by priority and date.
     *
     * @param size           maximum number of request to return
     * @param delayInSeconds delay in seconds from now of returned requests
     */
    default List<FeatureDeletionRequest> findRequestsToSchedule(int delayInSeconds, int size) {
        OffsetDateTime now = OffsetDateTime.now();
        return doFindRequestsToSchedule(FeatureRequestStep.LOCAL_DELAYED,
                                        now,
                                        now.minusSeconds(delayInSeconds),
                                        Pageable.ofSize(size));
    }

    /**
     * A deletion request can be scheduled only if the product is fully created. Thtat means :
     * - No creation request with same urn exists
     * - No dissemination update request exists with same urn. Indeed, after creation request is done the request is
     * deleted and the dissemination update requested are created. Product is fully created when dissemination info
     * update are done.
     */
    @Query("""
        SELECT request FROM FeatureDeletionRequest request
        WHERE NOT EXISTS (
            SELECT fcr.urn FROM FeatureCreationRequest fcr
            WHERE fcr.urn = request.urn
        )
        AND NOT EXISTS (
            SELECT fudr.urn FROM FeatureUpdateDisseminationRequest fudr
            WHERE fudr.urn = request.urn
        )
        AND request.step = :step
        AND request.registrationDate <= :delay
        AND request.requestDate <= :now
        ORDER BY request.priority desc, request.requestDate
        """)
    List<FeatureDeletionRequest> doFindRequestsToSchedule(@Param("step") FeatureRequestStep step,
                                                          @Param("now") OffsetDateTime now,
                                                          @Param("delay") OffsetDateTime delay,
                                                          Pageable pageLimit);

    @Modifying
    @Query("""
            UPDATE FeatureDeletionRequest fdr
            SET step = :step
            WHERE urn in (:urns)
        """)
    void updateStepByUrn(@Param("step") FeatureRequestStep step,
                         @Param("urns") Collection<FeatureUniformResourceName> urns);

    /**
     * clearAutomatically is set to ensure cache is cleared after this update. So all next find will find the updated
     * entity
     */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE FeatureDeletionRequest fdr
        SET forceDeletion = :forceDeletion
        WHERE fdr.id IN (:ids)
        AND step = :waitingStep
        """)
    void doForceDeletionById(@Param("ids") Set<Long> ids,
                             @Param("forceDeletion") boolean forceDeletion,
                             @Param("waitingStep") FeatureRequestStep waitingStep);

    default void forceDeletionById(Set<Long> ids) {
        doForceDeletionById(ids, true, FeatureRequestStep.WAITING_BLOCKING_DISSEMINATION);
    }
}
