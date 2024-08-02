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
package fr.cnes.regards.modules.model.dao;

import fr.cnes.regards.modules.model.domain.ModelAttrAssoc;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * {@link ModelAttrAssoc} repository
 *
 * @author Marc Sordi
 */
public interface IModelAttrAssocRepository extends JpaRepository<ModelAttrAssoc, Long> {

    List<ModelAttrAssoc> findByModelId(Long pModelId);

    @EntityGraph(attributePaths = { "attribute.properties" }, type = EntityGraph.EntityGraphType.LOAD)
    List<ModelAttrAssoc> findByModelName(String modelName);

    @EntityGraph(attributePaths = { "attribute.properties" }, type = EntityGraph.EntityGraphType.LOAD)
    ModelAttrAssoc findByModelIdAndAttribute(Long pModelId, AttributeModel pAttributeModel);

    @Override
    @EntityGraph(attributePaths = { "attribute.properties" }, type = EntityGraph.EntityGraphType.LOAD)
    List<ModelAttrAssoc> findAll();

    @Override
    @EntityGraph(attributePaths = { "attribute.properties" }, type = EntityGraph.EntityGraphType.LOAD)
    Optional<ModelAttrAssoc> findById(Long id);

    /**
     * Find page attribute which are associated to at least one of the models
     *
     * @return a page of attribute which are associated to at least one of the models
     */
    default Page<ModelAttrAssoc> findAllByModelNameIn(Collection<String> modelNames, Pageable pageable) {
        Page<Long> idPage = findIdPageByModelNameIn(modelNames, pageable);
        List<ModelAttrAssoc> modelAttrAssocs = findAllById(idPage.getContent());
        return new PageImpl<>(modelAttrAssocs, idPage.getPageable(), idPage.getTotalElements());
    }

    @Query("SELECT assoc.id FROM ModelAttrAssoc assoc WHERE assoc.model.name IN :modelNames")
    Page<Long> findIdPageByModelNameIn(@Param("modelNames") Collection<String> modelNames, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = { "attribute.properties" }, type = EntityGraph.EntityGraphType.LOAD)
    List<ModelAttrAssoc> findAllById(Iterable<Long> ids);

    /**
     * Find all the model attribute association which model is one of the given, represented by their ids
     *
     * @return the model attribute assocations
     */
    @EntityGraph(attributePaths = { "attribute.properties" }, type = EntityGraph.EntityGraphType.LOAD)
    Collection<ModelAttrAssoc> findAllByModelIdIn(Collection<Long> pModelsIds);

    /**
     * Find all model attribute associations by attribute id
     *
     * @return the model attribute associations
     */
    @EntityGraph(attributePaths = { "attribute.properties" }, type = EntityGraph.EntityGraphType.LOAD)
    Collection<ModelAttrAssoc> findAllByAttributeId(Long attrId);
}
