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
package fr.cnes.regards.modules.model.dao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;

/**
 *
 * {@link AttributeModel} repository
 *
 * @author Marc Sordi
 *
 */
@Repository
public interface IAttributeModelRepository
        extends CrudRepository<AttributeModel, Long>, JpaSpecificationExecutor<AttributeModel> {

    List<AttributeModel> findAll();

    @Override
    @EntityGraph(attributePaths = { "properties" })
    Optional<AttributeModel> findById(Long id);

    List<AttributeModel> findByType(PropertyType pType);

    List<AttributeModel> findByTypeAndFragmentName(PropertyType pType, String pFragmentName);

    List<AttributeModel> findByFragmentName(String pFragmentName);

    AttributeModel findByNameAndFragmentName(String pAttributeName, String pFragmentName);

    @EntityGraph(attributePaths = { "properties" })
    List<AttributeModel> findByFragmentId(Long pFragmentId);

    /**
     * Find attributes by name
     * @param fragmentName
     * @return attributes
     */
    Collection<AttributeModel> findByName(String fragmentName);

    @Override
    @EntityGraph(attributePaths = { "properties" })
    List<AttributeModel> findAll(Specification<AttributeModel> spec);
}
