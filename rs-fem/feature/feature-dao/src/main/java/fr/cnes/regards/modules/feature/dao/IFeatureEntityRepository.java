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

import fr.cnes.regards.modules.feature.domain.AbstractFeatureEntity;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.ILightFeatureEntity;
import fr.cnes.regards.modules.feature.domain.IUrnVersionByProvider;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Repository to handle access to {@link FeatureEntity} entities.
 *
 * @author Patrice FABRE
 */
@Repository
public interface IFeatureEntityRepository
    extends JpaRepository<FeatureEntity, Long>, JpaSpecificationExecutor<FeatureEntity> {

    @Modifying
    @Query(value = "delete from t_feature where id in (:ids)", nativeQuery = true)
    void deleteByIdIn(@Param("ids") List<Long> ids);

    FeatureEntity findTop1VersionByProviderIdOrderByVersionAsc(String providerId);

    FeatureEntity findByUrn(FeatureUniformResourceName urn);

    boolean existsByUrn(FeatureUniformResourceName featureUniformResourceName);

    long countByLastUpdateGreaterThan(OffsetDateTime from);

    /**
     * List existing provider identifiers in specified list
     */
    @Query(
        "select f.urn as urn, f.providerId as providerId, f.version as version from FeatureEntity f where f.providerId in :providerIds order by f.version desc")
    List<IUrnVersionByProvider> findByProviderIdInOrderByVersionDesc(@Param("providerIds") List<String> providerIds);

    /**
     * For dump purposes
     */
    Page<FeatureEntity> findByLastUpdateBetween(OffsetDateTime lastDumpDate, OffsetDateTime now, Pageable pageable);

    Page<FeatureEntity> findByLastUpdateLessThan(OffsetDateTime now, Pageable pageable);

    List<ILightFeatureEntity> findLightByUrnIn(Collection<FeatureUniformResourceName> uniformResourceNames);

    List<FeatureEntity> findCompleteByUrnIn(Collection<FeatureUniformResourceName> uniformResourceNames);

    void deleteAllByUrnIn(Collection<FeatureUniformResourceName> urns);

    Page<ILightFeatureEntity> findBySessionOwner(String sessionOwner, Pageable pageable);

    Page<ILightFeatureEntity> findBySessionOwnerAndSession(String sessionOwner, String session, Pageable pageable);


    /**
     * Filter from provided {@link FeatureUniformResourceName} urns all products that are waiting from an acknowledgment
     * on their dissemination
     */
    default Set<FeatureUniformResourceName> findFeatureUrnWaitingBlockingDissemination(Set<FeatureUniformResourceName> urns) {
        return findWaitingBlockingDissemination(urns)
                                      .stream()
                                      .map(AbstractFeatureEntity::getUrn)
                                      .collect(Collectors.toSet());
    }
    /**
     * Get all features having a running and blocking dissemination among provided urns
     * Be careful, this method do not fetch dissemination infos!
     */
    @Query("""
        SELECT feature FROM FeatureEntity feature
        WHERE EXISTS (
            SELECT 1 FROM FeatureDisseminationInfo fdi
            WHERE fdi.featureId = feature.id
            AND fdi.ackDate is NULL
            AND fdi.blocking is TRUE
        )
        AND feature.urn in (:urns)
        """)
    List<FeatureEntity> findWaitingBlockingDissemination(@Param("urns") Set<FeatureUniformResourceName> urns);

    @Modifying
    @Query(value = "truncate table t_feature CASCADE", nativeQuery = true)
    void deleteAll();
}
