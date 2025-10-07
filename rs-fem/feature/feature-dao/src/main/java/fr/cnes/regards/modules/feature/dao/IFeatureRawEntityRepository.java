/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.modules.feature.domain.FeatureRawEntity;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * Repository interface for managing {@link FeatureRawEntity} entities with theirs dissemination info. Those
 * entities
 * are
 * {@link fr.cnes.regards.modules.feature.domain.FeatureEntity} that are retrieved without deserializing the
 * feature field.
 *
 * @author Thibaud Michaudel
 */
@Repository
public interface IFeatureRawEntityRepository
    extends JpaRepository<FeatureRawEntity, Long>, JpaSpecificationExecutor<FeatureRawEntity> {

    @EntityGraph(attributePaths = { "disseminationsInfo" }, type = EntityGraph.EntityGraphType.LOAD)
    List<FeatureRawEntity> findByIdIn(Set<Long> ids, Sort sort);

    @EntityGraph(attributePaths = { "disseminationsInfo" }, type = EntityGraph.EntityGraphType.LOAD)
    FeatureRawEntity findByUrn(FeatureUniformResourceName urn);

    default List<FeatureRawEntity> findByIdInWithDisseminationsInfo(Set<Long> ids, Sort sort) {
        return findByIdIn(ids, sort);
    }

    default FeatureRawEntity findByUrnWithDisseminationsInfo(FeatureUniformResourceName urn) {
        return findByUrn(urn);
    }
}
