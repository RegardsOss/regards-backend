/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.Set;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.utils.SpecificationUtils;
import fr.cnes.regards.modules.feature.domain.request.AbstractFeatureRequest;
import fr.cnes.regards.modules.feature.domain.request.AbstractRequest;
import fr.cnes.regards.modules.feature.dto.FeatureRequestSearchParameters;

/**
 * JPA {@link Specification} to search for {@link AbstractRequest}s
 *
 * @author Léo Mieulet
 * @author Sébastien Binda
 */
public final class FeatureRequestSpecificationsHelper {

    /**
     * Creates search {@link Predicate}s for {@link Specification} search request about {@link AbstractFeatureRequest}s
     * @param filters {@link FeatureRequestSearchParameters}
     * @param root
     * @param query
     * @param cb
     * @param page {@link Pageable}
     * @return {@link Specification}
     */
    public static Set<Predicate> init(FeatureRequestSearchParameters filters, Root<?> root, CriteriaQuery<?> query,
            CriteriaBuilder cb, Pageable page) {
        Set<Predicate> predicates = Sets.newHashSet();

        if (filters.getStart() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("registrationDate"), filters.getStart()));
        }
        if (filters.getEnd() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("registrationDate"), filters.getEnd()));
        }
        if (filters.getUrn() != null) {
            predicates.add(cb.equal(root.get("urn"), filters.getUrn()));
        }
        if (filters.getState() != null) {
            predicates.add(cb.equal(root.get("state"), filters.getState()));
        }

        // Add order
        Sort.Direction defaultDirection = Sort.Direction.ASC;
        String defaultAttribute = "id";
        query.orderBy(SpecificationUtils.buildOrderBy(page, root, cb, defaultAttribute, defaultDirection));
        return predicates;
    }

}
