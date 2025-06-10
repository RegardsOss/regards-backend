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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.modules.feature.dao;

import fr.cnes.regards.modules.feature.domain.FeatureSimpleEntity;
import fr.cnes.regards.modules.feature.dto.FeatureIdUrnDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository in order to load {@link  FeatureIdUrnDto} projection from {@link FeatureSimpleEntity} entities.
 *
 * @author Stephane Cortine
 **/
@Repository
public class FeatureSimpleEntityCustomRepository implements IFeatureSimpleEntityCustomRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<FeatureIdUrnDto> findAll(Specification<FeatureSimpleEntity> spec, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Create main query
        CriteriaQuery<FeatureIdUrnDto> query = cb.createQuery(FeatureIdUrnDto.class);
        Root<FeatureSimpleEntity> root = query.from(FeatureSimpleEntity.class);

        // Create projection
        query.select(cb.construct(FeatureIdUrnDto.class, root.get("id"), root.get("urn")));

        if (spec != null) {
            Predicate predicate = spec.toPredicate(root, query, cb);
            if (predicate != null) {
                query.where(predicate);
            }
        }

        // Run main query with pagination
        TypedQuery<FeatureIdUrnDto> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        List<FeatureIdUrnDto> content = typedQuery.getResultList();

        // Create count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<FeatureSimpleEntity> countRoot = countQuery.from(FeatureSimpleEntity.class);
        countQuery.select(cb.countDistinct(countRoot));
        if (spec != null) {
            Predicate predicate = spec.toPredicate(countRoot, countQuery, cb);
            if (predicate != null) {
                countQuery.where(predicate);
            }
        }
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }
}
