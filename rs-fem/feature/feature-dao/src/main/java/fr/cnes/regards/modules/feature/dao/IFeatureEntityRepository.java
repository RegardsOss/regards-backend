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

import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.IUrnVersionByProvider;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;

@Repository
public interface IFeatureEntityRepository extends JpaRepository<FeatureEntity, Long> {

    FeatureEntity findTop1VersionByProviderIdOrderByVersionAsc(String providerId);

    FeatureEntity findByUrn(FeatureUniformResourceName urn);

    void deleteByUrnIn(Set<FeatureUniformResourceName> urns);

    List<FeatureEntity> findByUrnIn(List<FeatureUniformResourceName> urn);

    void deleteByIdIn(Set<Long> ids);

    // FIXME remove just for test
    long countByLastUpdateGreaterThan(OffsetDateTime from);

    /**
     * List existing provider identifiers in specified list
     */
    List<IUrnVersionByProvider> findByProviderIdInOrderByVersionDesc(List<String> providerIds);

    Page<FeatureEntity> findByModelAndLastUpdateAfter(String model, OffsetDateTime date, Pageable page);

    Page<FeatureEntity> findByModel(String model, Pageable page);

    @Modifying
    @Query(value = "UPDATE t_feature SET feature = jsonb_set(feature, CAST('{last}' AS text[]), CAST(CAST(:last AS text) AS jsonb)) WHERE urn IN :urns", nativeQuery = true)
    void updateLastByUrnIn(@Param("last") boolean last, @Param("urns") Set<String> urns);

    /**
     * For dump purposes
     */
    Page<FeatureEntity> findByLastUpdateBetween(OffsetDateTime lastDumpDate, OffsetDateTime now,
            Pageable pageable);

    Page<FeatureEntity> findByLastUpdateLessThan(OffsetDateTime now, Pageable pageable);
}
