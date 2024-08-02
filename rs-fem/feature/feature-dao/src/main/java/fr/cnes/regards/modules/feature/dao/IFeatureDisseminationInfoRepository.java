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

import fr.cnes.regards.modules.feature.domain.FeatureDisseminationInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

/**
 * JPA Repository to handle access to {@link FeatureDisseminationInfo} entities.
 *
 * @author Léo Mieulet
 */
public interface IFeatureDisseminationInfoRepository extends JpaRepository<FeatureDisseminationInfo, Long> {

    @Query(value = "SELECT * FROM t_feature_dissemination_info WHERE feature_id IN (:featureIds)", nativeQuery = true)
    Set<FeatureDisseminationInfo> findByFeatureIdIn(@Param("featureIds") List<Long> featureIds);

}
