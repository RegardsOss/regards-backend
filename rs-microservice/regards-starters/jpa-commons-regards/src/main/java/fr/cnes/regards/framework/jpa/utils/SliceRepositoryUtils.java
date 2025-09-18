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
package fr.cnes.regards.framework.jpa.utils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Common routines for repositories that return slices.
 *
 * @author Thibaud Michaudel
 */
public final class SliceRepositoryUtils {

    private SliceRepositoryUtils() {
    }

    /**
     * Finds a slice of entities based on the provided specification and slice size.
     */
    public static <T> Slice<T> findSlice(EntityManager entityManager,
                                         Class<T> entityClass,
                                         Specification<T> spec,
                                         Pageable pageable) {
        // need to use the criteria builder to support specifications
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = builder.createQuery(entityClass);
        Root<T> root = query.from(entityClass);

        Predicate predicate = spec != null ? spec.toPredicate(root, query, builder) : null;
        if (predicate != null) {
            query.where(predicate);
        }
        // don't forget to apply sorting
        List<Order> querySortOrders = new ArrayList<>();
        for (Sort.Order pageableSortOrder : pageable.getSort()) {
            // pageableSortOrder are sorted by priority
            Path<?> path = root.get(pageableSortOrder.getProperty());
            querySortOrders.add(pageableSortOrder.isAscending() ? builder.asc(path) : builder.desc(path));
        }
        query.orderBy(querySortOrders);

        List<T> content = entityManager.createQuery(query)
                                       .setFirstResult((int) pageable.getOffset())
                                       .setMaxResults(pageable.getPageSize()
                                                      + 1) // Fetch one extra to determine if there's a next slice
                                       .getResultList();
        boolean hasNext = content.size() > pageable.getPageSize();
        if (hasNext) {
            content = content.subList(0, pageable.getPageSize());
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }
}
