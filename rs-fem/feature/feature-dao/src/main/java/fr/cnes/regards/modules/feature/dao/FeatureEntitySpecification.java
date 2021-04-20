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

import javax.persistence.criteria.Predicate;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.utils.SpecificationUtils;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.dto.FeaturesSearchParameters;
import fr.cnes.regards.modules.feature.dto.FeaturesSelectionDTO;

/**
 * JPA {@link Specification} builder for {@link FeatureEntity}
 *
 * @author Sébastien Binda
 *
 */
public class FeatureEntitySpecification {

    private FeatureEntitySpecification() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Creates search {@link Specification} for {@link FeatureEntity}s
     * @param selection {@link FeaturesSearchParameters}
     * @param page {@link Pageable}
     * @return {@link Specification}
     */
    public static Specification<FeatureEntity> searchAllByFilters(FeaturesSelectionDTO selection, Pageable page) {
        return (root, query, cb) -> {

            Set<Predicate> predicates = Sets.newHashSet();
            FeaturesSearchParameters filters = selection.getFilters();

            if (filters != null) {
                if (filters.getModel() != null) {
                    predicates.add(cb.equal(root.get("model"), filters.getModel()));
                }
                if (filters.getSource() != null) {
                    predicates.add(cb.equal(root.get("sessionOwner"), filters.getSource()));
                }
                if (filters.getSession() != null) {
                    predicates.add(cb.equal(root.get("session"), filters.getSession()));
                }
                if (filters.getProviderId() != null) {
                    predicates.add(cb.equal(root.get("providerId"), filters.getProviderId()));
                    predicates.add(cb.like(cb.lower(root.get("providerId")),
                                           selection.getFilters().getProviderId().toLowerCase() + "%"));
                }
                if (filters.getFrom() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("lastUpdate"), filters.getFrom()));
                }
                if (filters.getTo() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("lastUpdate"), filters.getTo()));
                }
            }
            if (!selection.getFeatureIds().isEmpty()) {
                Set<Predicate> idsPredicates = Sets.newHashSet();
                switch (selection.getFeatureIdsSelectionMode()) {
                    case EXCLUDE:
                        selection.getFeatureIds()
                                .forEach(requestId -> idsPredicates.add(cb.notEqual(root.get("id"), requestId)));
                        break;
                    case INCLUDE:
                        selection.getFeatureIds()
                                .forEach(requestId -> idsPredicates.add(cb.equal(root.get("id"), requestId)));
                        break;
                    default:
                        break;
                }
                if (!idsPredicates.isEmpty()) {
                    predicates.add(cb.and(idsPredicates.toArray(new Predicate[idsPredicates.size()])));
                }
            }

            // Add order
            Sort.Direction defaultDirection = Sort.Direction.ASC;
            String defaultAttribute = "id";
            query.orderBy(SpecificationUtils.buildOrderBy(page, root, cb, defaultAttribute, defaultDirection));

            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

}
