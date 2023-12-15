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
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * Another repository to handle access to {@link FeatureEntity} entities, but this one always fetch disseminationsInfo
 * graph
 *
 * @author Léo Mieulet
 */
@Repository
public interface IFeatureEntityWithDisseminationRepository
    extends JpaRepository<FeatureEntity, Long>, JpaSpecificationExecutor<FeatureEntity> {

    default List<FeatureEntity> findByUrnIn(Set<FeatureUniformResourceName> urn) {
        return findByUrnIn(urn, Sort.unsorted());
    }

    @EntityGraph(attributePaths = { "disseminationsInfo" }, type = EntityGraph.EntityGraphType.LOAD)
    List<FeatureEntity> findByUrnIn(Set<FeatureUniformResourceName> urn, Sort sort);

    @EntityGraph(attributePaths = { "disseminationsInfo" }, type = EntityGraph.EntityGraphType.LOAD)
    List<FeatureEntity> findByIdIn(Set<Long> ids, Sort sort);

    @EntityGraph(attributePaths = { "disseminationsInfo" }, type = EntityGraph.EntityGraphType.LOAD)
    FeatureEntity findByUrn(FeatureUniformResourceName urn);

    @EntityGraph(attributePaths = { "disseminationsInfo" }, type = EntityGraph.EntityGraphType.LOAD)
    List<FeatureEntity> findAll();
}
