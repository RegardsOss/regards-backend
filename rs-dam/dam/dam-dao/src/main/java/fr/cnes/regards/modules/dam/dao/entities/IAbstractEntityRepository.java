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
package fr.cnes.regards.modules.dam.dao.entities;

import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import fr.cnes.regards.modules.model.domain.Model;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Common requests on entities
 *
 * @param <T> {@link AbstractEntity}
 * @author Sylvain Vissiere-Guerinet
 * @author oroussel
 */
public interface IAbstractEntityRepository<T extends AbstractEntity<?>>
    extends JpaRepository<T, Long>, JpaSpecificationExecutor<T> {

    /**
     * Find entity giving its id eagerly loading its common relations (ie relations defined into AbstractEntity
     *
     * @param pId id of entity
     * @return entity
     */
    @Override
    @EntityGraph(value = "graph.full.abstract.entity", type = EntityGraphType.LOAD)
    Optional<T> findById(Long pId);

    /**
     * Find all entities of which ipId belongs to given set (eagerly loading all relations)
     *
     * @param pIpIds set of ipId
     * @return found entities
     */
    @EntityGraph(attributePaths = { "tags", "groups", "model" }, type = EntityGraph.EntityGraphType.LOAD)
    List<T> findByIpIdIn(Set<UniformResourceName> pIpIds);

    /**
     * Find entity of given ipId
     *
     * @param pIpId ipId of which entity
     * @return found entity
     */
    T findOneByIpId(UniformResourceName pIpId);

    /**
     * Find entity of given IpId eagerly loading all common relations
     *
     * @param pIpId ipId of which entity
     * @return found entity
     */
    @EntityGraph(attributePaths = { "tags", "groups", "model" }, type = EntityGraph.EntityGraphType.LOAD)
    T findByIpId(UniformResourceName pIpId);

    /**
     * Find all entities complient with the given modelName
     *
     * @param pModelName name of the model we want to be complient with
     * @return entities complient with the given model
     */
    @EntityGraph(attributePaths = { "tags", "groups", "model" }, type = EntityGraph.EntityGraphType.LOAD)
    Set<T> findAllByModelName(String pModelName);

    /**
     * Find all entities matching given modelIds
     *
     * @param modelIds a list of model ids
     * @return entities complient with the given model
     */
    @EntityGraph(attributePaths = { "tags", "groups", "model" }, type = EntityGraph.EntityGraphType.LOAD)
    Set<T> findAllByModelIdIn(Set<Long> modelIds);

    /**
     * Count nb entities matching given modelIds
     *
     * @param modelIds a list of model ids
     * @return count of entities matching
     */
    long countByModelIdIn(Set<Long> modelIds);

    @EntityGraph(attributePaths = { "tags", "groups", "model" }, type = EntityGraph.EntityGraphType.LOAD)
    Set<T> findAllByModelNameIn(Collection<String> modelNames);

    @Override
    @EntityGraph(attributePaths = { "tags", "groups", "model" }, type = EntityGraph.EntityGraphType.LOAD)
    Page<T> findAll(Specification<T> spec, Pageable pageRequest);

    @Override
    default Page<T> findAll(Pageable pageable) {
        Page<Long> idPage = findIdPage(pageable);
        List<T> entities = findAllById(idPage.getContent());
        return new PageImpl<>(entities, idPage.getPageable(), idPage.getTotalElements());
    }

    @Query("select ae.id from AbstractEntity ae")
    Page<Long> findIdPage(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = { "tags", "groups", "model" }, type = EntityGraph.EntityGraphType.LOAD)
    List<T> findAllById(@Param("ids") Iterable<Long> ids);

    boolean existsByModel(Model model);

    /**
     * Check if at least one model is already linked to at least one entity
     *
     * @param modelIds model list
     * @return true if no entity exists linked with at least one model
     */
    default boolean isLinkedToEntities(Set<Long> modelIds) {
        return countByModelIdIn(modelIds) > 0;
    }

    /**
     * Find all entities containing given tag
     *
     * @param pTagToSearch tag to search entities for
     * @return entities which contain given tag
     */
    List<T> findByTags(String pTagToSearch);

    /**
     * Find the all the entity with this specified provider id
     *
     * @param providerId a provider id
     * @return entities corresponding to the provider id
     */
    @Query(value = "select * from {h-schema}t_entity where feature @> jsonb_build_object('providerId', ?1)",
           nativeQuery = true)
    Set<T> findAllByProviderId(String providerId);
}
